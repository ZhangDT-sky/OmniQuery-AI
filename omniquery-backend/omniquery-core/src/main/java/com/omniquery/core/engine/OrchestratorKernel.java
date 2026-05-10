package com.omniquery.core.engine;

import com.omniquery.core.config.OmniQueryProperties;
import com.omniquery.core.llm.SqlGenerationService;
import com.omniquery.core.model.QueryResponse;
import com.omniquery.core.model.QueryTrace;
import com.omniquery.core.service.JdbcQueryExecutor;
import com.omniquery.core.service.QueryIntentNormalizer;
import com.omniquery.core.session.CorrectionDetector;
import com.omniquery.core.session.QueryArtifact;
import com.omniquery.core.session.QueryMode;
import com.omniquery.core.session.QuerySessionStore;
import com.omniquery.core.session.QueryTurn;
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
    private final OmniQueryProperties properties;
    private final QuerySessionStore sessionStore;
    private final CorrectionDetector correctionDetector;

    public OrchestratorKernel(
        QueryIntentNormalizer normalizer,
        RetrievalService retrievalService,
        SqlGenerationService sqlGenerationService,
        SqlGuard sqlGuard,
        AclRewriter aclRewriter,
        JdbcQueryExecutor queryExecutor,
        OmniQueryProperties properties,
        QuerySessionStore sessionStore,
        CorrectionDetector correctionDetector
    ) {
        this.normalizer = normalizer;
        this.retrievalService = retrievalService;
        this.sqlGenerationService = sqlGenerationService;
        this.sqlGuard = sqlGuard;
        this.aclRewriter = aclRewriter;
        this.queryExecutor = queryExecutor;
        this.properties = properties;
        this.sessionStore = sessionStore;
        this.correctionDetector = correctionDetector;
    }

    public QueryResponse handle(String tenantId, String question) {
        return handle(tenantId, question, null);
    }

    public QueryResponse handle(String tenantId, String question, String sessionId) {
        List<QueryTrace> trace = new ArrayList<>();
        QueryMode mode = resolveMode(question, sessionId);
        String resolvedQuestion = resolveQuestion(question, sessionId, mode);
        try {
            var intent = normalizer.normalize(tenantId, resolvedQuestion);
            trace.add(new QueryTrace("intent", "Normalized user question", intent));
            if (QueryMode.CORRECTION == mode) {
                trace.add(new QueryTrace("session", "Resolved follow-up correction", resolvedQuestion));
            }

            var context = retrievalService.retrieve(intent.normalizedQuestion());
            trace.add(new QueryTrace("retrieval", "Retrieved schema and examples", context));

            var generated = sqlGenerationService.generate(intent, context);
            trace.add(new QueryTrace("generation", generated.explanation(), generated));

            var roles = Set.copyOf(intent.userContext().roles());
            var guardResult = sqlGuard.validate(generated.sql(), roles);
            if (!guardResult.allowed()) {
                trace.add(new QueryTrace("guard", "SQL rejected", guardResult.reason()));
                return saveAndRespond(sessionId, tenantId, mode, question, resolvedQuestion, generated, null, List.of(), false, guardResult.reason(), trace);
            }
            trace.add(new QueryTrace("guard", "SQL accepted", guardResult));

            var accessUser = new UserAccessContext(intent.userContext().userId(), intent.userContext().tenantId(), roles);
            var rewritten = aclRewriter.rewrite(guardResult.sql(), accessUser);
            trace.add(new QueryTrace("acl", "ACL policy applied", rewritten));

            var preflightRewrite = aclRewriter.rewrite(generated.sql(), accessUser);
            int expectedRows = queryExecutor.count(preflightRewrite.sql(), preflightRewrite.parameters());
            trace.add(new QueryTrace("preflight", "Estimated result size", expectedRows + " rows"));
            int maxRows = properties.security().getMaxRows();
            if (expectedRows > maxRows) {
                String error = "\u67e5\u8be2\u9884\u8ba1\u8fd4\u56de " + expectedRows
                    + " \u884c\uff0c\u8d85\u8fc7\u5f53\u524d\u4e0a\u9650 " + maxRows
                    + " \u884c\uff0c\u8bf7\u7f29\u5c0f\u67e5\u8be2\u8303\u56f4\u6216\u589e\u52a0\u7b5b\u9009\u6761\u4ef6\u3002";
                return saveAndRespond(sessionId, tenantId, mode, question, resolvedQuestion, generated, rewritten.sql(), List.of(), false, error, trace);
            }

            var rows = queryExecutor.query(rewritten.sql(), rewritten.parameters());
            trace.add(new QueryTrace("execution", "Query executed", rows.size() + " rows"));

            String answer = "Returned " + rows.size() + " row(s).";
            return saveAndRespond(sessionId, tenantId, mode, question, resolvedQuestion, generated, rewritten.sql(), rows, true, null, trace, answer);
        } catch (Exception e) {
            trace.add(new QueryTrace("error", "Pipeline failed", e.getMessage()));
            return saveAndRespond(sessionId, tenantId, mode, question, resolvedQuestion, null, null, List.of(), false, e.getMessage(), trace);
        }
    }

    private QueryMode resolveMode(String question, String sessionId) {
        boolean hasPreviousSuccess = sessionStore.find(sessionId)
            .flatMap(com.omniquery.core.session.QuerySession::lastSuccessfulTurn)
            .isPresent();
        return hasPreviousSuccess && correctionDetector.isCorrection(question) ? QueryMode.CORRECTION : QueryMode.NEW_QUERY;
    }

    private String resolveQuestion(String question, String sessionId, QueryMode mode) {
        if (QueryMode.CORRECTION != mode) {
            return question;
        }
        return sessionStore.find(sessionId)
            .flatMap(com.omniquery.core.session.QuerySession::lastSuccessfulTurn)
            .map(previous -> "Previous question: " + previous.resolvedQuestion()
                + "\nPrevious SQL: " + previous.artifact().sql()
                + "\nPrevious tables: " + previous.artifact().tables()
                + "\nPrevious columns: " + previous.artifact().columns()
                + "\nUser correction: " + question
                + "\nApply the correction with the smallest SQL change.")
            .orElse(question);
    }

    private QueryResponse saveAndRespond(
        String sessionId,
        String tenantId,
        QueryMode mode,
        String question,
        String resolvedQuestion,
        com.omniquery.core.model.GeneratedSql generated,
        String guardedSql,
        List<java.util.Map<String, Object>> rows,
        boolean success,
        String error,
        List<QueryTrace> trace
    ) {
        return saveAndRespond(sessionId, tenantId, mode, question, resolvedQuestion, generated, guardedSql, rows, success, error, trace, null);
    }

    private QueryResponse saveAndRespond(
        String sessionId,
        String tenantId,
        QueryMode mode,
        String question,
        String resolvedQuestion,
        com.omniquery.core.model.GeneratedSql generated,
        String guardedSql,
        List<java.util.Map<String, Object>> rows,
        boolean success,
        String error,
        List<QueryTrace> trace,
        String answer
    ) {
        QueryArtifact artifact = generated == null
            ? null
            : new QueryArtifact(
                generated.sql(),
                generated.tables(),
                generated.columns(),
                List.of(),
                List.of(),
                null,
                rows == null ? 0 : rows.size(),
                success
            );
        QueryTurn turn = QueryTurn.of(
            mode,
            question,
            resolvedQuestion,
            artifact,
            guardedSql,
            rows,
            success,
            error,
            trace
        );
        var session = sessionStore.append(sessionId, tenantId, turn);
        return new QueryResponse(session.sessionId(), turn.turnId(), mode, success, answer, generated == null ? null : generated.sql(), guardedSql, rows, error, trace);
    }
}
