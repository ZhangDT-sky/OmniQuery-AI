package com.omniquery.rag.service;

import com.omniquery.rag.model.ExampleSqlDocument;
import com.omniquery.rag.model.SchemaDocument;
import com.omniquery.rag.repository.DemoKnowledgeBase;
import com.omniquery.rag.vector.VectorSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorBackedRetrieverTest {

    @Test
    void returnsVectorResultsBeforeKeywordFallback() {
        SchemaDocument schema = new SchemaDocument("invoices", "Invoices", List.of("id BIGINT"), List.of(), List.of("user"));
        SchemaRetriever retriever = new SchemaRetriever(new DemoKnowledgeBase(), provider(new StubVectorSearch(List.of(schema), List.of())));

        var results = retriever.retrieve("orders", 3);

        assertEquals(List.of(schema), results);
    }

    @Test
    void fallsBackToKeywordResultsWhenVectorReturnsEmpty() {
        SchemaRetriever retriever = new SchemaRetriever(new DemoKnowledgeBase(), provider(new StubVectorSearch(List.of(), List.of())));

        var results = retriever.retrieve("orders", 3);

        assertTrue(results.stream().anyMatch(schema -> schema.tableName().equals("orders")));
    }

    private ObjectProvider<VectorSearchService> provider(VectorSearchService service) {
        return new ObjectProvider<>() {
            @Override
            public VectorSearchService getObject(Object... args) {
                return service;
            }

            @Override
            public VectorSearchService getIfAvailable() {
                return service;
            }

            @Override
            public VectorSearchService getIfUnique() {
                return service;
            }

            @Override
            public VectorSearchService getObject() {
                return service;
            }

            @Override
            public void forEach(Consumer<? super VectorSearchService> action) {
                action.accept(service);
            }

            @Override
            public Stream<VectorSearchService> stream() {
                return Stream.of(service);
            }

            @Override
            public VectorSearchService getIfAvailable(Supplier<VectorSearchService> defaultSupplier) {
                return service;
            }

            @Override
            public VectorSearchService getIfUnique(Supplier<VectorSearchService> defaultSupplier) {
                return service;
            }
        };
    }

    private record StubVectorSearch(List<SchemaDocument> schemas, List<ExampleSqlDocument> examples) implements VectorSearchService {
        @Override
        public List<SchemaDocument> retrieveSchemas(String question, int limit) {
            return schemas;
        }

        @Override
        public List<ExampleSqlDocument> retrieveExamples(String question, int limit) {
            return examples;
        }
    }
}
