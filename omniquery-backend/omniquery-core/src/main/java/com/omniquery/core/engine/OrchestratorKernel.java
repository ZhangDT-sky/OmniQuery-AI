package com.omniquery.core.engine;

import com.omniquery.core.llm.SqlGenerationService;
import com.omniquery.core.model.QueryResponse;
import com.omniquery.core.model.QueryTrace;
import com.omniquery.core.service.JdbcQueryExecutor;
import com.omniquery.core.service.QueryIntentNormalizer;
import com.omniquery.rag.service.RetrievalService;
import com.omniquery.security.AclRewriter;
import com.omniquery.security.SqlGuard;
import com.omniquery.security.model.UserAccessContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class OrchestratorKernel {

    private final QueryIntentNormalizer normalizer;
    private final RetrievalService retrievalService;
    private final SqlGenerationService sqlGenerationService;
    private final SqlGuard sqlGuard;
    private final AclRewriter aclRewriter;
    private final JdbcQueryExecutor queryExecutor;

    public OrchestratorKernel(
        QueryIntentNormalizer normalizer,
        RetrievalService retrievalService,
        SqlGenerationService sqlGenerationService,
        SqlGuard sqlGuard,
        AclRewriter aclRewriter,
        JdbcQueryExecutor queryExecutor
    ) {
        this.normalizer = normalizer;
        this.retrievalService = retrievalService;
        this.sqlGenerationService = sqlGenerationService;
        this.sqlGuard = sqlGuard;
        this.aclRewriter = aclRewriter;
        this.queryExecutor = queryExecutor;
    }

    public QueryResponse handle(String tenantId, String question) {
        List<QueryTrace> trace = new ArrayList<>();
        try {
            var intent = normalizer.normalize(tenantId, question);
            trace.add(new QueryTrace("intent", "Normalized user question", intent));

            var context = retrievalService.retrieve(intent.normalizedQuestion());
            trace.add(new QueryTrace("retrieval", "Retrieved schema and examples", context));

            var generated = sqlGenerationService.generate(intent, context);
            trace.add(new QueryTrace("generation", generated.explanation(), generated));

            var roles = Set.copyOf(intent.userContext().roles());
            var guardResult = sqlGuard.validate(generated.sql(), roles);
            if (!guardResult.allowed()) {
                trace.add(new QueryTrace("guard", "SQL rejected", guardResult.reason()));
                return new QueryResponse(false, null, generated.sql(), null, List.of(), guardResult.reason(), trace);
            }
            trace.add(new QueryTrace("guard", "SQL accepted", guardResult));

            var accessUser = new UserAccessContext(intent.userContext().userId(), intent.userContext().tenantId(), roles);
            var rewritten = aclRewriter.rewrite(guardResult.sql(), accessUser);
            trace.add(new QueryTrace("acl", "ACL policy applied", rewritten));

            var rows = queryExecutor.query(rewritten.sql(), rewritten.parameters());
            trace.add(new QueryTrace("execution", "Query executed", rows.size() + " rows"));

            String answer = "Returned " + rows.size() + " row(s).";
            return new QueryResponse(true, answer, generated.sql(), rewritten.sql(), rows, null, trace);
        } catch (Exception e) {
            trace.add(new QueryTrace("error", "Pipeline failed", e.getMessage()));
            return new QueryResponse(false, null, null, null, List.of(), e.getMessage(), trace);
        }
    }
}
