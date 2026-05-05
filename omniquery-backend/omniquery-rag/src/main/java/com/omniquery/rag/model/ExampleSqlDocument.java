package com.omniquery.rag.model;

import java.util.List;

public record ExampleSqlDocument(
    String question,
    String sql,
    List<String> tables,
    String explanation
) {
    public String searchableText() {
        return String.join(" ", question, sql, String.join(" ", tables), explanation);
    }
}
