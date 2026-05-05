package com.omniquery.security;

import com.omniquery.security.model.SchemaPolicy;
import com.omniquery.security.model.TablePolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlGuardTest {

    private final SchemaPolicy policy = new SchemaPolicy(Map.of(
        "customers", new TablePolicy("customers", Set.of("id", "name", "tenant_id", "created_by"), Set.of("admin", "user")),
        "orders", new TablePolicy("orders", Set.of("id", "customer_id", "status", "total_amount", "tenant_id", "created_by", "created_at"), Set.of("admin", "user"))
    ));

    @Test
    void allowsSelectAndAddsLimit() {
        SqlGuard guard = new SqlGuard(policy, 100);
        var result = guard.validate("SELECT id, status FROM orders", Set.of("user"));
        assertTrue(result.allowed());
        assertTrue(result.sql().toUpperCase().contains("LIMIT 100"));
    }

    @Test
    void rejectsMutations() {
        SqlGuard guard = new SqlGuard(policy, 100);
        var result = guard.validate("DELETE FROM orders WHERE id = 1", Set.of("user"));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Only SELECT"));
    }

    @Test
    void rejectsUnknownColumns() {
        SqlGuard guard = new SqlGuard(policy, 100);
        var result = guard.validate("SELECT password FROM customers", Set.of("user"));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Unknown column"));
    }

    @Test
    void rejectsWildcardSelects() {
        SqlGuard guard = new SqlGuard(policy, 100);
        var result = guard.validate("SELECT * FROM orders", Set.of("user"));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Wildcard"));
    }

    @Test
    void rejectsUnknownColumnsInWhereAndOrderBy() {
        SqlGuard guard = new SqlGuard(policy, 100);
        var whereResult = guard.validate("SELECT id FROM orders WHERE secret_flag = 1", Set.of("user"));
        var orderResult = guard.validate("SELECT id FROM orders ORDER BY internal_score", Set.of("user"));

        assertFalse(whereResult.allowed());
        assertTrue(whereResult.reason().contains("Unknown column"));
        assertFalse(orderResult.allowed());
        assertTrue(orderResult.reason().contains("Unknown column"));
    }

    @Test
    void rejectsDangerousFunctions() {
        SqlGuard guard = new SqlGuard(policy, 100);
        var result = guard.validate("SELECT id FROM orders WHERE SLEEP(1) = 0", Set.of("user"));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Dangerous function"));
    }

    @Test
    void rejectsTooManyJoins() {
        SqlGuard guard = new SqlGuard(policy, 100, 1, Set.of());
        var result = guard.validate("""
            SELECT o.id
            FROM orders o
            JOIN customers c1 ON c1.id = o.customer_id
            JOIN customers c2 ON c2.id = o.customer_id
            """, Set.of("user"));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Too many joins"));
    }

    @Test
    void capsExistingLimit() {
        SqlGuard guard = new SqlGuard(policy, 100);
        var result = guard.validate("SELECT id FROM orders LIMIT 10000", Set.of("user"));
        assertTrue(result.allowed());
        assertTrue(result.sql().toUpperCase().contains("LIMIT 100"));
    }
}
