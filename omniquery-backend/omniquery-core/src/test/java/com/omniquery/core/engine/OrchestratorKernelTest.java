package com.omniquery.core.engine;

import com.omniquery.core.config.OmniQueryProperties;
import com.omniquery.core.llm.FallbackSqlGenerationService;
import com.omniquery.core.service.JdbcQueryExecutor;
import com.omniquery.core.service.QueryIntentNormalizer;
import com.omniquery.core.session.CorrectionDetector;
import com.omniquery.core.session.InMemoryQuerySessionStore;
import com.omniquery.core.session.QueryMode;
import com.omniquery.rag.repository.DemoKnowledgeBase;
import com.omniquery.rag.service.ExampleRetriever;
import com.omniquery.rag.service.RetrievalService;
import com.omniquery.rag.service.SchemaRetriever;
import com.omniquery.security.AclRewriter;
import com.omniquery.security.SqlGuard;
import com.omniquery.security.model.AccessPolicy;
import com.omniquery.security.model.SchemaPolicy;
import com.omniquery.security.model.TablePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrchestratorKernelTest {

    @Test
    void runsEndToEndWithAclFilteredRows() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:kernel_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE customers (id BIGINT PRIMARY KEY, name VARCHAR(100), tenant_id VARCHAR(50), created_by VARCHAR(50))");
        jdbc.execute("CREATE TABLE orders (id BIGINT PRIMARY KEY, customer_id BIGINT, status VARCHAR(30), total_amount DECIMAL(12,2), tenant_id VARCHAR(50), created_by VARCHAR(50), created_at TIMESTAMP)");
        jdbc.execute("INSERT INTO customers VALUES (1, 'Acme Corp', 'tenant_a', 'u1'), (2, 'Globex', 'tenant_b', 'u2')");
        jdbc.execute("INSERT INTO orders VALUES (1001, 1, 'PAID', 1200.00, 'tenant_a', 'u1', CURRENT_TIMESTAMP), (1002, 2, 'PAID', 9999.00, 'tenant_b', 'u2', CURRENT_TIMESTAMP)");

        DemoKnowledgeBase kb = new DemoKnowledgeBase();
        RetrievalService retrieval = new RetrievalService(new SchemaRetriever(kb), new ExampleRetriever(kb));
        SchemaPolicy schemaPolicy = new SchemaPolicy(Map.of(
            "customers", new TablePolicy("customers", Set.of("id", "name", "tenant_id", "created_by"), Set.of("admin", "user")),
            "orders", new TablePolicy("orders", Set.of("id", "customer_id", "status", "total_amount", "tenant_id", "created_by", "created_at"), Set.of("admin", "user"))
        ));
        OmniQueryProperties properties = new OmniQueryProperties();
        OrchestratorKernel kernel = new OrchestratorKernel(
            new QueryIntentNormalizer(),
            retrieval,
            new FallbackSqlGenerationService(),
            new SqlGuard(schemaPolicy, 100),
            new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id", "customers", "tenant_id"))),
            new JdbcQueryExecutor(jdbc, properties),
            properties,
            new InMemoryQuerySessionStore(properties),
            new CorrectionDetector()
        );

        var response = kernel.handle("tenant_a", "show total paid amount by customer");

        assertTrue(response.success(), response.error());
        assertEquals(1, response.rows().size());
        assertTrue(response.guardedSql().toLowerCase().contains("tenant_id"));
        assertFalse(response.rows().toString().contains("9999"));
    }

    @Test
    void rejectsQueryWhenPreflightCountExceedsMaxRows() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:kernel_preflight_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE customers (id BIGINT PRIMARY KEY, name VARCHAR(100), tenant_id VARCHAR(50), created_by VARCHAR(50))");
        jdbc.execute("CREATE TABLE orders (id BIGINT PRIMARY KEY, customer_id BIGINT, status VARCHAR(30), total_amount DECIMAL(12,2), tenant_id VARCHAR(50), created_by VARCHAR(50), created_at TIMESTAMP)");
        jdbc.execute("INSERT INTO customers VALUES (1, 'Acme Corp', 'tenant_a', 'u1')");
        jdbc.execute("INSERT INTO orders VALUES (1001, 1, 'PAID', 1200.00, 'tenant_a', 'u1', CURRENT_TIMESTAMP), (1002, 1, 'PAID', 900.00, 'tenant_a', 'u1', CURRENT_TIMESTAMP)");

        DemoKnowledgeBase kb = new DemoKnowledgeBase();
        RetrievalService retrieval = new RetrievalService(new SchemaRetriever(kb), new ExampleRetriever(kb));
        SchemaPolicy schemaPolicy = new SchemaPolicy(Map.of(
            "customers", new TablePolicy("customers", Set.of("id", "name", "tenant_id", "created_by"), Set.of("admin", "user")),
            "orders", new TablePolicy("orders", Set.of("id", "customer_id", "status", "total_amount", "tenant_id", "created_by", "created_at"), Set.of("admin", "user"))
        ));
        OmniQueryProperties properties = new OmniQueryProperties();
        properties.security().setMaxRows(1);
        OrchestratorKernel kernel = new OrchestratorKernel(
            new QueryIntentNormalizer(),
            retrieval,
            new FallbackSqlGenerationService(),
            new SqlGuard(schemaPolicy, 100),
            new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id", "customers", "tenant_id"))),
            new JdbcQueryExecutor(jdbc, properties),
            properties,
            new InMemoryQuerySessionStore(properties),
            new CorrectionDetector()
        );

        var response = kernel.handle("tenant_a", "show recent orders with customer names");

        assertFalse(response.success());
        assertEquals(0, response.rows().size());
        assertTrue(response.error().contains("\u9884\u8ba1\u8fd4\u56de 2 \u884c"));
        assertTrue(response.trace().stream().anyMatch(step -> step.phase().equals("preflight")));
    }

    @Test
    void appliesCorrectionWithinSameSession() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:kernel_session_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE customers (id BIGINT PRIMARY KEY, name VARCHAR(100), tenant_id VARCHAR(50), created_by VARCHAR(50))");
        jdbc.execute("CREATE TABLE orders (id BIGINT PRIMARY KEY, customer_id BIGINT, status VARCHAR(30), total_amount DECIMAL(12,2), tenant_id VARCHAR(50), created_by VARCHAR(50), created_at TIMESTAMP)");
        jdbc.execute("INSERT INTO customers VALUES (1, 'Acme Corp', 'tenant_a', 'u1')");
        jdbc.execute("INSERT INTO orders VALUES (1001, 1, 'PAID', 1200.00, 'tenant_a', 'u1', CURRENT_TIMESTAMP), (1002, 1, 'PENDING', 900.00, 'tenant_a', 'u1', CURRENT_TIMESTAMP)");

        DemoKnowledgeBase kb = new DemoKnowledgeBase();
        RetrievalService retrieval = new RetrievalService(new SchemaRetriever(kb), new ExampleRetriever(kb));
        SchemaPolicy schemaPolicy = new SchemaPolicy(Map.of(
            "customers", new TablePolicy("customers", Set.of("id", "name", "tenant_id", "created_by"), Set.of("admin", "user")),
            "orders", new TablePolicy("orders", Set.of("id", "customer_id", "status", "total_amount", "tenant_id", "created_by", "created_at"), Set.of("admin", "user"))
        ));
        OmniQueryProperties properties = new OmniQueryProperties();
        InMemoryQuerySessionStore sessionStore = new InMemoryQuerySessionStore(properties);
        OrchestratorKernel kernel = new OrchestratorKernel(
            new QueryIntentNormalizer(),
            retrieval,
            new FallbackSqlGenerationService(),
            new SqlGuard(schemaPolicy, 100),
            new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id", "customers", "tenant_id"))),
            new JdbcQueryExecutor(jdbc, properties),
            properties,
            sessionStore,
            new CorrectionDetector()
        );

        var first = kernel.handle("tenant_a", "show recent orders with customer names");
        var second = kernel.handle("tenant_a", "\u53ea\u770b PAID \u72b6\u6001", first.sessionId());

        assertEquals(QueryMode.CORRECTION, second.mode());
        assertEquals(first.sessionId(), second.sessionId());
        assertEquals(1, second.rows().size());
        assertTrue(second.rows().toString().contains("PAID"));
        assertFalse(second.rows().toString().contains("PENDING"));
        assertEquals(2, sessionStore.find(first.sessionId()).orElseThrow().turns().size());
    }

    @Test
    void correctionFallsBackToNewQueryWhenSessionHasNoSuccessfulTurn() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:kernel_failed_session_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE customers (id BIGINT PRIMARY KEY, name VARCHAR(100), tenant_id VARCHAR(50), created_by VARCHAR(50))");
        jdbc.execute("CREATE TABLE orders (id BIGINT PRIMARY KEY, customer_id BIGINT, status VARCHAR(30), total_amount DECIMAL(12,2), tenant_id VARCHAR(50), created_by VARCHAR(50), created_at TIMESTAMP)");
        jdbc.execute("INSERT INTO customers VALUES (1, 'Acme Corp', 'tenant_a', 'u1')");
        jdbc.execute("INSERT INTO orders VALUES (1001, 1, 'PAID', 1200.00, 'tenant_a', 'u1', CURRENT_TIMESTAMP)");

        DemoKnowledgeBase kb = new DemoKnowledgeBase();
        RetrievalService retrieval = new RetrievalService(new SchemaRetriever(kb), new ExampleRetriever(kb));
        SchemaPolicy schemaPolicy = new SchemaPolicy(Map.of(
            "customers", new TablePolicy("customers", Set.of("id", "name", "tenant_id", "created_by"), Set.of("admin", "user")),
            "orders", new TablePolicy("orders", Set.of("id", "customer_id", "status", "total_amount", "tenant_id", "created_by", "created_at"), Set.of("admin", "user"))
        ));
        OmniQueryProperties properties = new OmniQueryProperties();
        properties.security().setMaxRows(0);
        InMemoryQuerySessionStore sessionStore = new InMemoryQuerySessionStore(properties);
        OrchestratorKernel kernel = new OrchestratorKernel(
            new QueryIntentNormalizer(),
            retrieval,
            new FallbackSqlGenerationService(),
            new SqlGuard(schemaPolicy, 100),
            new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id", "customers", "tenant_id"))),
            new JdbcQueryExecutor(jdbc, properties),
            properties,
            sessionStore,
            new CorrectionDetector()
        );

        var failed = kernel.handle("tenant_a", "show recent orders with customer names");
        var followUp = kernel.handle("tenant_a", "\u53ea\u770b PAID \u72b6\u6001", failed.sessionId());

        assertFalse(failed.success());
        assertEquals(QueryMode.NEW_QUERY, followUp.mode());
    }
}
