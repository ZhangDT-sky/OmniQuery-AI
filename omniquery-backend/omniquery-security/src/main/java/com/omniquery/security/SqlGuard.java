package com.omniquery.security;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.druid.util.JdbcConstants;
import com.omniquery.security.model.SchemaPolicy;
import com.omniquery.security.model.SqlGuardResult;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class SqlGuard {

    private final SchemaPolicy schemaPolicy;
    private final int maxRows;
    private final int maxJoins;
    private final Set<String> dangerousFunctions;

    public SqlGuard(SchemaPolicy schemaPolicy, int maxRows) {
        this(schemaPolicy, maxRows, 3, Set.of("sleep", "benchmark", "load_file"));
    }

    public SqlGuard(SchemaPolicy schemaPolicy, int maxRows, int maxJoins, Set<String> dangerousFunctions) {
        this.schemaPolicy = schemaPolicy;
        this.maxRows = maxRows;
        this.maxJoins = maxJoins;
        this.dangerousFunctions = dangerousFunctions.isEmpty()
            ? Set.of("sleep", "benchmark", "load_file")
            : dangerousFunctions.stream().map(name -> name.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
    }

    public SqlGuardResult validate(String sql, Set<String> roles) {
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        if (statements.size() != 1) {
            return denied("Expected exactly one SQL statement");
        }
        if (!(statements.get(0) instanceof SQLSelectStatement selectStatement)) {
            return denied("Only SELECT statements are allowed");
        }
        if (!(selectStatement.getSelect().getQuery() instanceof SQLSelectQueryBlock queryBlock)) {
            return denied("Only simple SELECT query blocks are supported");
        }
        if (containsDangerousFunction(sql)) {
            return denied("Dangerous function is not allowed");
        }
        if (containsWildcard(queryBlock)) {
            return denied("Wildcard SELECT is not allowed");
        }
        int joinCount = countJoins(queryBlock.getFrom());
        if (joinCount > maxJoins) {
            return denied("Too many joins: " + joinCount);
        }

        Set<String> tables = new HashSet<>();
        collectTables(queryBlock.getFrom(), tables);
        for (String table : tables) {
            var tablePolicy = schemaPolicy.tables().get(table.toLowerCase());
            if (tablePolicy == null) {
                return denied("Unknown table: " + table);
            }
            boolean roleAllowed = roles.stream().anyMatch(tablePolicy.roles()::contains);
            if (!roleAllowed) {
                return denied("Role cannot access table: " + table);
            }
        }

        Set<String> columns = collectReferencedColumns(selectStatement);
        for (String column : columns) {
            boolean known = tables.stream()
                .map(table -> schemaPolicy.tables().get(table.toLowerCase()))
                .anyMatch(policy -> policy.columns().contains(column.toLowerCase()));
            if (!known) {
                return denied("Unknown column: " + column);
            }
        }

        enforceLimit(queryBlock);
        String guardedSql = SQLUtils.toSQLString(statements, JdbcConstants.MYSQL);
        return new SqlGuardResult(true, guardedSql, null, tables, columns);
    }

    private SqlGuardResult denied(String reason) {
        return new SqlGuardResult(false, null, reason, Set.of(), Set.of());
    }

    private void collectTables(SQLTableSource source, Set<String> tables) {
        if (source instanceof SQLExprTableSource tableSource) {
            tables.add(tableSource.getTableName().toLowerCase());
        } else if (source instanceof SQLJoinTableSource join) {
            collectTables(join.getLeft(), tables);
            collectTables(join.getRight(), tables);
        }
    }

    private Set<String> collectReferencedColumns(SQLSelectStatement selectStatement) {
        SchemaStatVisitor visitor = SQLUtils.createSchemaStatVisitor(JdbcConstants.MYSQL);
        selectStatement.accept(visitor);
        Set<String> columns = new HashSet<>();
        for (TableStat.Column column : visitor.getColumns()) {
            String name = column.getName();
            if (name != null && !name.equals("*")) {
                columns.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    private boolean containsWildcard(SQLSelectQueryBlock queryBlock) {
        return queryBlock.getSelectList().stream()
            .anyMatch(item -> item.getExpr().toString().contains("*"));
    }

    private boolean containsDangerousFunction(String sql) {
        return dangerousFunctions.stream()
            .anyMatch(function -> Pattern.compile("\\b" + Pattern.quote(function) + "\\s*\\(", Pattern.CASE_INSENSITIVE)
                .matcher(sql)
                .find());
    }

    private int countJoins(SQLTableSource source) {
        if (source instanceof SQLJoinTableSource join) {
            return 1 + countJoins(join.getLeft()) + countJoins(join.getRight());
        }
        return 0;
    }

    private void enforceLimit(SQLSelectQueryBlock queryBlock) {
        if (queryBlock.getLimit() == null || currentLimit(queryBlock.getLimit()) > maxRows) {
            queryBlock.setLimit(new SQLLimit(new SQLNumberExpr(maxRows)));
        }
    }

    private int currentLimit(SQLLimit limit) {
        if (limit.getRowCount() == null) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(limit.getRowCount().toString());
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }
}
