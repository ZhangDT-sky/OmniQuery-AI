package com.omniquery.core.session;

import com.omniquery.core.model.QueryTrace;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record QueryTurn(
    String turnId,
    String mode,
    String userQuestion,
    String resolvedQuestion,
    String rawSql,
    String guardedSql,
    List<String> tables,
    List<String> columns,
    int rowCount,
    boolean success,
    String error,
    List<QueryTrace> trace,
    Instant createdAt
) {
    public static QueryTurn of(
        String mode,
        String userQuestion,
        String resolvedQuestion,
        String rawSql,
        String guardedSql,
        List<String> tables,
        List<String> columns,
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
            rawSql,
            guardedSql,
            tables,
            columns,
            rows == null ? 0 : rows.size(),
            success,
            error,
            List.copyOf(trace),
            Instant.now()
        );
    }
}
