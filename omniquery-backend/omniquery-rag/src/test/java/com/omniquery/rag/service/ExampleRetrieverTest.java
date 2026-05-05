package com.omniquery.rag.service;

import com.omniquery.rag.repository.DemoKnowledgeBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExampleRetrieverTest {

    @Test
    void retrievesGoldenSqlExamplesForRevenueQuestion() {
        ExampleRetriever retriever = new ExampleRetriever(new DemoKnowledgeBase());

        var results = retriever.retrieve("total paid amount by customer", 2);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).sql().toLowerCase().contains("sum"));
    }
}
