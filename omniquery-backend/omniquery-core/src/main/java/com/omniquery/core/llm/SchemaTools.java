package com.omniquery.core.llm;

import com.omniquery.rag.service.ExampleRetriever;
import com.omniquery.rag.service.SchemaRetriever;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class SchemaTools {

    private final SchemaRetriever schemaRetriever;
    private final ExampleRetriever exampleRetriever;

    public SchemaTools(SchemaRetriever schemaRetriever, ExampleRetriever exampleRetriever) {
        this.schemaRetriever = schemaRetriever;
        this.exampleRetriever = exampleRetriever;
    }

    @Tool("Find relevant database schema fragments for a natural language question. This tool is read-only.")
    public String findSchema(String question) {
        return schemaRetriever.retrieve(question, 5).toString();
    }

    @Tool("Find relevant NL2SQL examples for a natural language question. This tool is read-only.")
    public String findExamples(String question) {
        return exampleRetriever.retrieve(question, 3).toString();
    }
}
