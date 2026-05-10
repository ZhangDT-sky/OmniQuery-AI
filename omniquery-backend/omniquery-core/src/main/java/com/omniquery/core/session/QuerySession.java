package com.omniquery.core.session;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record QuerySession(
    String sessionId,
    String tenantId,
    List<QueryTurn> turns,
    Instant createdAt,
    Instant updatedAt
) {
    public Optional<QueryTurn> lastSuccessfulTurn() {
        for (int i = turns.size() - 1; i >= 0; i--) {
            QueryTurn turn = turns.get(i);
            if (turn.success() && turn.artifact() != null && turn.artifact().success()) {
                return Optional.of(turn);
            }
        }
        return Optional.empty();
    }
}
