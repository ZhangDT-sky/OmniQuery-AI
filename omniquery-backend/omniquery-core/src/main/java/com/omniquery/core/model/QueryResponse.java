package com.omniquery.core.model;

import java.util.List;
import java.util.Map;

public record QueryResponse(
    String sessionId,
    String turnId,
    String mode,
    boolean success,
    String answer,
    String rawSql,
    String guardedSql,
    List<Map<String, Object>> rows,
    String error,
    List<QueryTrace> trace
) {
    public QueryResponse(
        boolean success,
        String answer,
        String rawSql,
        String guardedSql,
        List<Map<String, Object>> rows,
        String error,
        List<QueryTrace> trace
    ) {
        this(null, null, "NEW_QUERY", success, answer, rawSql, guardedSql, rows, error, trace);
    }
}
