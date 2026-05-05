package com.omniquery.rag.service;

import com.omniquery.rag.repository.DemoKnowledgeBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaRetrieverTest {

    @Test
    void retrievesRelevantSchemaByTableAndColumnWords() {
        SchemaRetriever retriever = new SchemaRetriever(new DemoKnowledgeBase());

        var results = retriever.retrieve("show recent orders and customer names", 3);

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(doc -> doc.tableName().equals("orders")));
        assertTrue(results.stream().anyMatch(doc -> doc.tableName().equals("customers")));
    }
}
