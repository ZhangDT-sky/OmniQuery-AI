package com.omniquery.rag.repository;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExampleSqlCatalogTest {

    @Test
    void convertsConfiguredExamplesToDocuments() {
        ExampleSqlCatalog catalog = new ExampleSqlCatalog();
        ExampleSqlCatalog.ConfiguredExample example = new ExampleSqlCatalog.ConfiguredExample();
        example.setQuestion("recent orders");
        example.setSql("SELECT id FROM orders LIMIT 100");
        example.setTables(List.of("orders"));
        example.setExplanation("Fetch recent orders");
        catalog.setExamples(List.of(example));

        var documents = catalog.documents();

        assertEquals(1, documents.size());
        assertEquals("recent orders", documents.get(0).question());
        assertEquals("SELECT id FROM orders LIMIT 100", documents.get(0).sql());
        assertEquals(List.of("orders"), documents.get(0).tables());
        assertEquals("Fetch recent orders", documents.get(0).explanation());
    }
}
