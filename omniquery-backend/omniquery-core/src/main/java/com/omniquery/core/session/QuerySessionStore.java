package com.omniquery.core.session;

import java.util.Optional;

public interface QuerySessionStore {
    QuerySession append(String sessionId, String tenantId, QueryTurn turn);

    Optional<QuerySession> find(String sessionId);
}
