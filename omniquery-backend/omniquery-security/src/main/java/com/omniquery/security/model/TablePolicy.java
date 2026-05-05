package com.omniquery.security.model;

import java.util.Set;

public record TablePolicy(String tableName, Set<String> columns, Set<String> roles) {}
