package com.omniquery.rag.vector;

import com.omniquery.rag.model.ExampleSqlDocument;
import com.omniquery.rag.model.SchemaDocument;
import com.omniquery.rag.repository.KnowledgeBase;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PgVectorSearchService implements VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(PgVectorSearchService.class);

    private final KnowledgeBase knowledgeBase;
    private final TextEmbeddingClient embeddingClient;
    private final PgVectorStore vectorStore;
    private final Map<String, SchemaDocument> schemasById = new LinkedHashMap<>();
    private final Map<String, ExampleSqlDocument> examplesById = new LinkedHashMap<>();
    private volatile boolean ready;

    public PgVectorSearchService(KnowledgeBase knowledgeBase, TextEmbeddingClient embeddingClient, PgVectorStore vectorStore) {
        this.knowledgeBase = knowledgeBase;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void indexKnowledgeBase() {
        try {
            vectorStore.initialize();
            for (SchemaDocument schema : knowledgeBase.schemas()) {
                String id = "schema:" + schema.tableName();
                schemasById.put(id, schema);
                vectorStore.upsert(id, "schema", schema.searchableText(), embeddingClient.embed(schema.searchableText()));
            }
            for (ExampleSqlDocument example : knowledgeBase.examples()) {
                String id = "example:" + Integer.toHexString(example.searchableText().hashCode());
                examplesById.put(id, example);
                vectorStore.upsert(id, "example", example.searchableText(), embeddingClient.embed(example.searchableText()));
            }
            ready = true;
            log.info("Indexed {} schema documents and {} example documents into pgvector", schemasById.size(), examplesById.size());
        } catch (RuntimeException ex) {
            ready = false;
            log.warn("Vector RAG indexing failed; keyword retrieval will be used as fallback: {}", ex.getMessage());
        }
    }

    @Override
    public List<SchemaDocument> retrieveSchemas(String question, int limit) {
        if (!ready) {
            return List.of();
        }
        try {
            return vectorStore.searchIds("schema", embeddingClient.embed(question), limit).stream()
                .map(schemasById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        } catch (RuntimeException ex) {
            log.warn("Vector schema retrieval failed; keyword retrieval will be used as fallback: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public List<ExampleSqlDocument> retrieveExamples(String question, int limit) {
        if (!ready) {
            return List.of();
        }
        try {
            return vectorStore.searchIds("example", embeddingClient.embed(question), limit).stream()
                .map(examplesById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        } catch (RuntimeException ex) {
            log.warn("Vector example retrieval failed; keyword retrieval will be used as fallback: {}", ex.getMessage());
            return List.of();
        }
    }
}
