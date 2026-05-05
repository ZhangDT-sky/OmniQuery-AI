package com.omniquery.core.model;

import java.util.List;
import java.util.Map;

public record QueryResponse(
    boolean success,
    String answer,
    String rawSql,
    String guardedSql,
    List<Map<String, Object>> rows,
    String error,
    List<QueryTrace> trace
) {}
