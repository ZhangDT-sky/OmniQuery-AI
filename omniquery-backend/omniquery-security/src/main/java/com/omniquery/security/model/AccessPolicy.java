package com.omniquery.security.model;

import java.util.Map;

public record AccessPolicy(Map<String, String> tenantColumns) {}
