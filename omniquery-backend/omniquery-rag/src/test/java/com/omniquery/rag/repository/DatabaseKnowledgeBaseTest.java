package com.omniquery.rag.repository;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseKnowledgeBaseTest {

    @Test
    void scansQueryableTablesAndColumnsFromJdbcMetadata() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:metadata_scan;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "sa",
            ""
        );
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE invoices (id BIGINT PRIMARY KEY, total_amount DECIMAL(12,2), tenant_id VARCHAR(50))");

        DatabaseKnowledgeBase knowledgeBase = new DatabaseKnowledgeBase(dataSource);

        var schemas = knowledgeBase.schemas();

        assertEquals(1, schemas.size());
        assertEquals("invoices", schemas.get(0).tableName());
        assertTrue(schemas.get(0).columns().contains("id BIGINT"));
        assertTrue(schemas.get(0).columns().contains("total_amount DECIMAL"));
        assertTrue(schemas.get(0).columns().contains("tenant_id CHARACTER VARYING"));
    }

    @Test
    void returnsConfiguredExamplesForExternalDatabases() {
        ExampleSqlCatalog catalog = new ExampleSqlCatalog();
        ExampleSqlCatalog.ConfiguredExample example = new ExampleSqlCatalog.ConfiguredExample();
        example.setQuestion("paid orders by customer");
        example.setSql("SELECT c.name FROM customers c JOIN orders o ON o.customer_id = c.id WHERE o.status = 'PAID' LIMIT 100");
        example.setTables(java.util.List.of("customers", "orders"));
        example.setExplanation("Join customers and orders for paid orders");
        catalog.setExamples(java.util.List.of(example));

        DatabaseKnowledgeBase knowledgeBase = new DatabaseKnowledgeBase(new DriverManagerDataSource(), catalog);

        var examples = knowledgeBase.examples();

        assertEquals(1, examples.size());
        assertEquals("paid orders by customer", examples.get(0).question());
        assertEquals(java.util.List.of("customers", "orders"), examples.get(0).tables());
    }
}
