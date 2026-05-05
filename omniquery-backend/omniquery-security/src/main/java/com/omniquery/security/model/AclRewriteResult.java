package com.omniquery.security.model;

import java.util.List;

public record AclRewriteResult(String sql, List<Object> parameters) {}
