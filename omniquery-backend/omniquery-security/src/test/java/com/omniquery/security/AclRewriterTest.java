package com.omniquery.security;

import com.omniquery.security.model.AccessPolicy;
import com.omniquery.security.model.UserAccessContext;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AclRewriterTest {

    @Test
    void injectsAclWhenWhereIsMissing() {
        AclRewriter rewriter = new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id")));

        var result = rewriter.rewrite(
            "SELECT id, status FROM orders LIMIT 100",
            new UserAccessContext("u1", "tenant_a", Set.of("user"))
        );

        assertTrue(result.sql().toLowerCase().contains("where tenant_id = ?"));
        assertTrue(result.sql().toLowerCase().contains("limit 100"));
        assertEquals("tenant_a", result.parameters().get(0));
    }

    @Test
    void appendsAclToExistingWhere() {
        AclRewriter rewriter = new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id")));

        var result = rewriter.rewrite(
            "SELECT id FROM orders WHERE status = 'PAID' LIMIT 100",
            new UserAccessContext("u1", "tenant_a", Set.of("user"))
        );

        assertTrue(result.sql().toLowerCase().contains("status = 'paid'"));
        assertTrue(result.sql().toLowerCase().contains("and tenant_id = ?"));
        assertEquals(List.of("tenant_a"), result.parameters());
    }

    @Test
    void adminBypassesAcl() {
        AclRewriter rewriter = new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id")));

        var result = rewriter.rewrite(
            "SELECT id FROM orders LIMIT 100",
            new UserAccessContext("admin", "tenant_a", Set.of("admin"))
        );

        assertFalse(result.sql().toLowerCase().contains("tenant_id ="));
        assertTrue(result.parameters().isEmpty());
    }
}
