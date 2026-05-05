package com.omniquery.rag.repository;

import com.omniquery.rag.model.ExampleSqlDocument;
import com.omniquery.rag.model.SchemaDocument;

import java.util.List;

public interface KnowledgeBase {

    List<SchemaDocument> schemas();

    List<ExampleSqlDocument> examples();
}
