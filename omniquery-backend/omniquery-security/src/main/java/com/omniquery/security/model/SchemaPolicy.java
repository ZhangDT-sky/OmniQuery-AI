package com.omniquery.security.model;

import java.util.Map;

public record SchemaPolicy(Map<String, TablePolicy> tables) {}
