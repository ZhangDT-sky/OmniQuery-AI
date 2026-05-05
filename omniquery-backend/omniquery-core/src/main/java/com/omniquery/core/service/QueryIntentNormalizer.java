package com.omniquery.core.service;

import com.omniquery.core.model.QueryIntent;
import com.omniquery.sdk.model.UserContext;

import java.util.List;

public class QueryIntentNormalizer {

    public QueryIntent normalize(String tenantId, String question) {
        UserContext user = new UserContext("default-user", tenantId, List.of("user"));
        return new QueryIntent(question, question.trim(), "mysql", user);
    }
}
