package com.omniquery.core.session;

import com.omniquery.core.model.QueryTrace;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record QueryTurn(
    String turnId,
    QueryMode mode,
    String userQuestion,
    String resolvedQuestion,
    QueryArtifact artifact,
    String guardedSql,
    int rowCount,
    boolean success,
    String error,
    List<QueryTrace> trace,
    Instant createdAt
) {
    public static QueryTurn of(
        QueryMode mode,
        String userQuestion,
        String resolvedQuestion,
        QueryArtifact artifact,
        String guardedSql,
        List<Map<String, Object>> rows,
        boolean success,
        String error,
        List<QueryTrace> trace
    ) {
        return new QueryTurn(
            java.util.UUID.randomUUID().toString(),
            mode,
            userQuestion,
            resolvedQuestion,
            artifact,
            guardedSql,
            rows == null ? 0 : rows.size(),
            success,
            error,
            List.copyOf(trace),
            Instant.now()
        );
    }
}
