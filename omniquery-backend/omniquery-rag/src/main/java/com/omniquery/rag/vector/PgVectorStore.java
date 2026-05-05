package com.omniquery.rag.vector;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class PgVectorStore {

    private final JdbcTemplate jdbcTemplate;
    private final int dimensions;

    public PgVectorStore(JdbcTemplate jdbcTemplate, int dimensions) {
        this.jdbcTemplate = jdbcTemplate;
        this.dimensions = dimensions;
    }

    public void initialize() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS omniquery_rag_documents (
                id VARCHAR(200) PRIMARY KEY,
                kind VARCHAR(40) NOT NULL,
                text TEXT NOT NULL,
                embedding vector(%d) NOT NULL
            )
            """.formatted(dimensions));
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS omniquery_rag_documents_embedding_idx ON omniquery_rag_documents USING hnsw (embedding vector_cosine_ops)");
    }

    public void upsert(String id, String kind, String text, float[] embedding) {
        jdbcTemplate.update("""
            INSERT INTO omniquery_rag_documents (id, kind, text, embedding)
            VALUES (?, ?, ?, ?::vector)
            ON CONFLICT (id) DO UPDATE SET kind = EXCLUDED.kind, text = EXCLUDED.text, embedding = EXCLUDED.embedding
            """, id, kind, text, PgVectorLiteral.from(embedding));
    }

    public List<String> searchIds(String kind, float[] embedding, int limit) {
        return jdbcTemplate.queryForList("""
            SELECT id
            FROM omniquery_rag_documents
            WHERE kind = ?
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """, String.class, kind, PgVectorLiteral.from(embedding), limit);
    }
}
