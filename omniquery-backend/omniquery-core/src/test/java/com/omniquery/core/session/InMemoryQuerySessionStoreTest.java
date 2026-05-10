package com.omniquery.core.session;

import com.omniquery.core.config.OmniQueryProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryQuerySessionStoreTest {

    @Test
    void keepsOnlyConfiguredNumberOfTurnsAndFindsLastSuccessfulTurn() {
        OmniQueryProperties properties = new OmniQueryProperties();
        properties.session().setMaxTurns(2);
        InMemoryQuerySessionStore store = new InMemoryQuerySessionStore(properties);

        QueryTurn firstSuccess = turn(QueryMode.NEW_QUERY, true, "SELECT id FROM orders");
        QueryTurn failedRepair = turn(QueryMode.NEW_QUERY, false, "SELECT missing FROM orders");
        QueryTurn lastSuccess = turn(QueryMode.CORRECTION, true, "SELECT id FROM orders WHERE status = 'PAID'");

        QuerySession session = store.append(null, "tenant_a", firstSuccess);
        store.append(session.sessionId(), "tenant_a", failedRepair);
        store.append(session.sessionId(), "tenant_a", lastSuccess);

        QuerySession stored = store.find(session.sessionId()).orElseThrow();
        assertEquals(2, stored.turns().size());
        assertEquals(lastSuccess.turnId(), stored.lastSuccessfulTurn().orElseThrow().turnId());
        assertTrue(stored.turns().stream().noneMatch(turn -> turn.turnId().equals(firstSuccess.turnId())));
    }

    private QueryTurn turn(QueryMode mode, boolean success, String sql) {
        return QueryTurn.of(
            mode,
            "question",
            "resolved",
            new QueryArtifact(sql, List.of("orders"), List.of("id"), List.of(), List.of(), null, success ? 1 : 0, success),
            sql,
            List.of(),
            success,
            success ? null : "failed",
            List.of()
        );
    }
}
