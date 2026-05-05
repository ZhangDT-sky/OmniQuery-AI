package com.omniquery.security.model;

import java.util.Set;

public record SqlGuardResult(
    boolean allowed,
    String sql,
    String reason,
    Set<String> tables,
    Set<String> columns
) {}
