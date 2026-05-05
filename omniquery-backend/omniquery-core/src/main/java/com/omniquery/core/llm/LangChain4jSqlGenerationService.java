package com.omniquery.core.llm;

import com.omniquery.core.model.GeneratedSql;
import com.omniquery.core.model.QueryIntent;
import com.omniquery.rag.model.RetrievedContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("llm")
public class LangChain4jSqlGenerationService implements SqlGenerationService {

    private final Nl2SqlAssistant assistant;

    public LangChain4jSqlGenerationService(Nl2SqlAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public GeneratedSql generate(QueryIntent intent, RetrievedContext context) {
        return assistant.generate(intent.normalizedQuestion(), context.toPromptContext()).toGeneratedSql();
    }
}
