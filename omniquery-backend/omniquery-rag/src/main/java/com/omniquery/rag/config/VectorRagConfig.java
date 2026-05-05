package com.omniquery.rag.config;

import com.omniquery.rag.repository.KnowledgeBase;
import com.omniquery.rag.vector.DashScopeEmbeddingClient;
import com.omniquery.rag.vector.PgVectorSearchService;
import com.omniquery.rag.vector.PgVectorStore;
import com.omniquery.rag.vector.TextEmbeddingClient;
import com.omniquery.rag.vector.VectorSearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
@Profile("vector-rag")
public class VectorRagConfig {

    @Bean
    DataSource vectorDataSource(
        @Value("${omniquery.rag.vector.jdbc-url}") String jdbcUrl,
        @Value("${omniquery.rag.vector.username}") String username,
        @Value("${omniquery.rag.vector.password}") String password
    ) {
        return new DriverManagerDataSource(jdbcUrl, username, password);
    }

    @Bean
    PgVectorStore pgVectorStore(DataSource vectorDataSource, @Value("${omniquery.rag.vector.dimensions}") int dimensions) {
        return new PgVectorStore(new JdbcTemplate(vectorDataSource), dimensions);
    }

    @Bean
    TextEmbeddingClient textEmbeddingClient(
        @Value("${omniquery.rag.embedding.api-key}") String apiKey,
        @Value("${omniquery.rag.embedding.base-url}") String baseUrl,
        @Value("${omniquery.rag.embedding.model}") String model,
        @Value("${omniquery.rag.vector.dimensions}") int dimensions
    ) {
        return new DashScopeEmbeddingClient(apiKey, baseUrl, model, dimensions);
    }

    @Bean
    VectorSearchService vectorSearchService(KnowledgeBase knowledgeBase, TextEmbeddingClient textEmbeddingClient, PgVectorStore pgVectorStore) {
        return new PgVectorSearchService(knowledgeBase, textEmbeddingClient, pgVectorStore);
    }
}
