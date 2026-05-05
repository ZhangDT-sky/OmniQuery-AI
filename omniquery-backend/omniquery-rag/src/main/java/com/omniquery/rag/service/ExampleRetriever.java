package com.omniquery.rag.service;

import com.omniquery.rag.model.ExampleSqlDocument;
import com.omniquery.rag.repository.KnowledgeBase;
import com.omniquery.rag.vector.VectorSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ExampleRetriever {

    private final KnowledgeBase knowledgeBase;
    private final VectorSearchService vectorSearchService;

    public ExampleRetriever(KnowledgeBase knowledgeBase) {
        this(knowledgeBase, null);
    }

    @Autowired
    public ExampleRetriever(KnowledgeBase knowledgeBase, ObjectProvider<VectorSearchService> vectorSearchService) {
        this.knowledgeBase = knowledgeBase;
        this.vectorSearchService = vectorSearchService == null ? null : vectorSearchService.getIfAvailable();
    }

    public List<ExampleSqlDocument> retrieve(String question, int limit) {
        if (vectorSearchService != null) {
            List<ExampleSqlDocument> vectorResults = vectorSearchService.retrieveExamples(question, limit);
            if (!vectorResults.isEmpty()) {
                return vectorResults;
            }
        }
        String normalized = question.toLowerCase();
        return knowledgeBase.examples().stream()
            .sorted(Comparator.comparingInt((ExampleSqlDocument doc) -> score(doc.searchableText(), normalized)).reversed())
            .filter(doc -> score(doc.searchableText(), normalized) > 0)
            .limit(limit)
            .toList();
    }

    private int score(String text, String question) {
        String lower = text.toLowerCase();
        int score = 0;
        for (String token : question.split("\\W+")) {
            if (!token.isBlank() && lower.contains(token)) {
                score++;
            }
        }
        return score;
    }
}
