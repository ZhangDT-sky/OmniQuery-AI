package com.omniquery.core.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface Nl2SqlAssistant {

    @SystemMessage("""
        You are OmniQuery's NL2SQL generator.
        Return only a structured object with sql, tables, columns, and explanation.
        The sql must be one read-only SELECT statement.
        Do not use SELECT *.
        Use only tables and columns present in the provided context.
        Add no ACL predicates yourself; the backend applies tenant ACL after validation.
        Prefer simple SQL that can pass an AST safety gateway.
        """)
    @UserMessage("""
        Question:
        {{question}}

        Retrieved schema and examples:
        {{context}}
        """)
    SqlGenerationOutput generate(@V("question") String question, @V("context") String context);
}
