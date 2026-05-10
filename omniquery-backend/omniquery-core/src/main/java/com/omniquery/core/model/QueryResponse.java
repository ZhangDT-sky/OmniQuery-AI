package com.omniquery.core.model;

import com.omniquery.core.session.QueryMode;

import java.util.List;
import java.util.Map;

public record QueryResponse(
    String sessionId,
    String turnId,
    QueryMode mode,
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
        this(null, null, QueryMode.NEW_QUERY, success, answer, rawSql, guardedSql, rows, error, trace);
    }
}
