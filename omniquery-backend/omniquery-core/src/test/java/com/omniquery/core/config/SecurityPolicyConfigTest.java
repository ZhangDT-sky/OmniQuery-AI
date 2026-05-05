package com.omniquery.core.config;

import com.omniquery.rag.model.ExampleSqlDocument;
import com.omniquery.rag.model.SchemaDocument;
import com.omniquery.rag.repository.KnowledgeBase;
import com.omniquery.security.model.UserAccessContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityPolicyConfigTest {

    @Test
    void buildsSchemaAndAclPoliciesFromKnowledgeBase() {
        KnowledgeBase knowledgeBase = new KnowledgeBase() {
            @Override
            public List<SchemaDocument> schemas() {
                return List.of(new SchemaDocument(
                    "invoices",
                    "Invoices",
                    List.of("id BIGINT", "total_amount DECIMAL", "tenant_id VARCHAR"),
                    List.of(),
                    List.of("admin", "analyst")
                ));
            }

            @Override
            public List<ExampleSqlDocument> examples() {
                return List.of();
            }
        };
        SecurityPolicyConfig config = new SecurityPolicyConfig();
        OmniQueryProperties properties = new OmniQueryProperties();
        properties.security().setMaxRows(25);
        properties.security().setMaxJoins(2);

        var schemaPolicy = config.schemaPolicy(knowledgeBase);
        var guard = config.sqlGuard(schemaPolicy, properties);
        var aclRewriter = config.aclRewriter(knowledgeBase);

        assertTrue(schemaPolicy.tables().containsKey("invoices"));
        assertEquals("invoices", schemaPolicy.tables().get("invoices").tableName());
        assertTrue(schemaPolicy.tables().get("invoices").columns().contains("total_amount"));
        assertTrue(guard.validate("SELECT id FROM invoices", Set.of("analyst")).sql().toUpperCase().contains("LIMIT 25"));
        assertTrue(aclRewriter.rewrite("SELECT id FROM invoices", new UserAccessContext("u1", "tenant_a", Set.of("analyst"))).sql()
            .toLowerCase()
            .contains("tenant_id = ?"));
    }
}
