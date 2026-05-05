package com.omniquery.rag.service;

import com.omniquery.rag.model.RetrievedContext;
import org.springframework.stereotype.Service;

@Service
public class RetrievalService {

    private final SchemaRetriever schemaRetriever;
    private final ExampleRetriever exampleRetriever;

    public RetrievalService(SchemaRetriever schemaRetriever, ExampleRetriever exampleRetriever) {
        this.schemaRetriever = schemaRetriever;
        this.exampleRetriever = exampleRetriever;
    }

    public RetrievedContext retrieve(String question) {
        return new RetrievedContext(
            schemaRetriever.retrieve(question, 5),
            exampleRetriever.retrieve(question, 3)
        );
    }
}
