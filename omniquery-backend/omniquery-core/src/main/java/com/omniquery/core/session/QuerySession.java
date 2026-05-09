package com.omniquery.core.session;

import java.time.Instant;
import java.util.List;

public record QuerySession(
    String sessionId,
    String tenantId,
    List<QueryTurn> turns,
    Instant createdAt,
    Instant updatedAt
) {}
