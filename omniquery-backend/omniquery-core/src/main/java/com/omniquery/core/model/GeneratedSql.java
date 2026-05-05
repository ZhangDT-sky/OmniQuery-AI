package com.omniquery.core.model;

import java.util.List;

public record GeneratedSql(
    String sql,
    List<String> tables,
    List<String> columns,
    String explanation
) {}
