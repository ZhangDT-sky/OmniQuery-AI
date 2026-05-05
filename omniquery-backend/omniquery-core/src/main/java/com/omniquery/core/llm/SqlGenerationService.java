package com.omniquery.core.llm;

import com.omniquery.core.model.GeneratedSql;
import com.omniquery.core.model.QueryIntent;
import com.omniquery.rag.model.RetrievedContext;

public interface SqlGenerationService {
    GeneratedSql generate(QueryIntent intent, RetrievedContext context);
}
