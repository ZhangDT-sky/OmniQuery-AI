package com.omniquery.rag.model;

import java.util.List;

public record RetrievedContext(
    List<SchemaDocument> schemas,
    List<ExampleSqlDocument> examples
) {
    public String toPromptContext() {
        StringBuilder builder = new StringBuilder();
        builder.append("SCHEMA:\n");
        schemas.forEach(schema -> builder
            .append("- Table ").append(schema.tableName())
            .append(": ").append(schema.description())
            .append("; columns=").append(schema.columns())
            .append("; relationships=").append(schema.relationships())
            .append("\n"));
        builder.append("EXAMPLES:\n");
        examples.forEach(example -> builder
            .append("- Q: ").append(example.question())
            .append("\n  SQL: ").append(example.sql())
            .append("\n"));
        return builder.toString();
    }
}
