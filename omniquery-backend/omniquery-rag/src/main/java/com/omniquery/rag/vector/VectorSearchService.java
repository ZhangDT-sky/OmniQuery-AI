package com.omniquery.rag.vector;

import com.omniquery.rag.model.ExampleSqlDocument;
import com.omniquery.rag.model.SchemaDocument;

import java.util.List;

public interface VectorSearchService {

    List<SchemaDocument> retrieveSchemas(String question, int limit);

    List<ExampleSqlDocument> retrieveExamples(String question, int limit);
}
