package com.omniquery.core.session;

import com.omniquery.core.config.OmniQueryProperties;
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

    private final ConcurrentHashMap<String, QuerySession> sessions = new ConcurrentHashMap<>();
    private final OmniQueryProperties properties;

    public InMemoryQuerySessionStore(OmniQueryProperties properties) {
        this.properties = properties;
    }

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
            int maxTurns = Math.max(1, properties.session().getMaxTurns());
            if (turns.size() > maxTurns) {
                turns = turns.subList(turns.size() - maxTurns, turns.size());
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
        int ttlMinutes = Math.max(1, properties.session().getTtlMinutes());
        return session.updatedAt().plus(Duration.ofMinutes(ttlMinutes)).isBefore(now);
    }
}
