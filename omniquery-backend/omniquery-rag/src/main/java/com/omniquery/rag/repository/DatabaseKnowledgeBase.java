package com.omniquery.rag.repository;

import com.omniquery.rag.model.ExampleSqlDocument;
import com.omniquery.rag.model.SchemaDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
@Profile("external-db")
public class DatabaseKnowledgeBase implements KnowledgeBase {

    private static final List<String> DEFAULT_ROLES = List.of("admin", "user");

    private final DataSource dataSource;
    private final ExampleSqlCatalog exampleSqlCatalog;

    public DatabaseKnowledgeBase(DataSource dataSource) {
        this(dataSource, new ExampleSqlCatalog());
    }

    @Autowired
    public DatabaseKnowledgeBase(DataSource dataSource, ExampleSqlCatalog exampleSqlCatalog) {
        this.dataSource = dataSource;
        this.exampleSqlCatalog = exampleSqlCatalog;
    }

    @Override
    public List<SchemaDocument> schemas() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String catalog = connection.getCatalog();
            Map<String, MutableTable> tables = scanTables(metadata, catalog);
            for (MutableTable table : tables.values()) {
                table.columns.addAll(scanColumns(metadata, catalog, table.name));
                table.relationships.addAll(scanImportedKeys(metadata, catalog, table.name));
            }
            return tables.values().stream()
                .filter(table -> !table.columns.isEmpty())
                .map(MutableTable::toDocument)
                .toList();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to scan database metadata", ex);
        }
    }

    @Override
    public List<ExampleSqlDocument> examples() {
        return exampleSqlCatalog.documents();
    }

    private Map<String, MutableTable> scanTables(DatabaseMetaData metadata, String catalog) throws SQLException {
        Map<String, MutableTable> tables = new LinkedHashMap<>();
        try (ResultSet rs = metadata.getTables(catalog, null, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM");
                String table = normalize(rs.getString("TABLE_NAME"));
                if (table == null || isSystemTable(schema, table)) {
                    continue;
                }
                tables.put(table, new MutableTable(table));
            }
        }
        return tables;
    }

    private List<String> scanColumns(DatabaseMetaData metadata, String catalog, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (ResultSet rs = metadata.getColumns(catalog, null, table, "%")) {
            while (rs.next()) {
                String name = normalize(rs.getString("COLUMN_NAME"));
                String type = rs.getString("TYPE_NAME");
                if (name != null && type != null) {
                    columns.add(name + " " + type.toUpperCase(Locale.ROOT));
                }
            }
        }
        return columns;
    }

    private List<String> scanImportedKeys(DatabaseMetaData metadata, String catalog, String table) throws SQLException {
        List<String> relationships = new ArrayList<>();
        try (ResultSet rs = metadata.getImportedKeys(catalog, null, table)) {
            while (rs.next()) {
                String fkTable = normalize(rs.getString("FKTABLE_NAME"));
                String fkColumn = normalize(rs.getString("FKCOLUMN_NAME"));
                String pkTable = normalize(rs.getString("PKTABLE_NAME"));
                String pkColumn = normalize(rs.getString("PKCOLUMN_NAME"));
                if (fkTable != null && fkColumn != null && pkTable != null && pkColumn != null) {
                    relationships.add(fkTable + "." + fkColumn + " = " + pkTable + "." + pkColumn);
                }
            }
        }
        return relationships;
    }

    private boolean isSystemTable(String schema, String table) {
        String lowerSchema = schema == null ? "" : schema.toLowerCase(Locale.ROOT);
        String lowerTable = table.toLowerCase(Locale.ROOT);
        return lowerSchema.equals("information_schema")
            || lowerSchema.equals("pg_catalog")
            || lowerSchema.equals("sys")
            || lowerSchema.equals("mysql")
            || lowerTable.startsWith("pg_")
            || lowerTable.startsWith("sql_");
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.toLowerCase(Locale.ROOT);
    }

    private static class MutableTable {
        private final String name;
        private final List<String> columns = new ArrayList<>();
        private final List<String> relationships = new ArrayList<>();

        private MutableTable(String name) {
            this.name = name;
        }

        private SchemaDocument toDocument() {
            return new SchemaDocument(name, "Database table " + name, columns, relationships, DEFAULT_ROLES);
        }
    }
}
