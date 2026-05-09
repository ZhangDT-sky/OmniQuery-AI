package com.omniquery.core.session;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryQuerySessionStore implements QuerySessionStore {

    private static final int MAX_TURNS = 10;
    private static final Duration TTL = Duration.ofMinutes(30);

    private final ConcurrentHashMap<String, QuerySession> sessions = new ConcurrentHashMap<>();

    @Override
    public QuerySession append(String sessionId, String tenantId, QueryTurn turn) {
        cleanupExpired();
        String effectiveSessionId = sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;
        Instant now = Instant.now();
        return sessions.compute(effectiveSessionId, (id, existing) -> {
            if (existing == null || isExpired(existing, now)) {
                return new QuerySession(id, tenantId, List.of(turn), now, now);
            }
            List<QueryTurn> turns = new ArrayList<>(existing.turns());
            turns.add(turn);
            if (turns.size() > MAX_TURNS) {
                turns = turns.subList(turns.size() - MAX_TURNS, turns.size());
            }
            return new QuerySession(id, tenantId, List.copyOf(turns), existing.createdAt(), now);
        });
    }

    @Override
    public Optional<QuerySession> find(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        QuerySession session = sessions.get(sessionId);
        if (session == null || isExpired(session, Instant.now())) {
            sessions.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
    }

    private boolean isExpired(QuerySession session, Instant now) {
        return session.updatedAt().plus(TTL).isBefore(now);
    }
}
