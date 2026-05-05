package com.omniquery.rag.model;

import java.util.List;

public record SchemaDocument(
    String tableName,
    String description,
    List<String> columns,
    List<String> relationships,
    List<String> roles
) {
    public String searchableText() {
        return String.join(" ", tableName, description, String.join(" ", columns), String.join(" ", relationships));
    }
}
