package com.omniquery.core.session;

import java.util.List;

public record QueryArtifact(
    String sql,
    List<String> tables,
    List<String> columns,
    List<String> filters,
    List<String> orderBy,
    Integer limit,
    int rowCount,
    boolean success
) {
    public QueryArtifact {
        tables = tables == null ? List.of() : List.copyOf(tables);
        columns = columns == null ? List.of() : List.copyOf(columns);
        filters = filters == null ? List.of() : List.copyOf(filters);
        orderBy = orderBy == null ? List.of() : List.copyOf(orderBy);
    }
}
