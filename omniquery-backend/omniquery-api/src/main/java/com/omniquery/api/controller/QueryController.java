package com.omniquery.api.controller;

import com.omniquery.core.engine.OrchestratorKernel;
import com.omniquery.core.model.QueryResponse;
import com.omniquery.core.session.QuerySession;
import com.omniquery.core.session.QuerySessionStore;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
public class QueryController {

    private final OrchestratorKernel kernel;
    private final QuerySessionStore sessionStore;

    public QueryController(OrchestratorKernel kernel, QuerySessionStore sessionStore) {
        this.kernel = kernel;
        this.sessionStore = sessionStore;
    }

    @PostMapping("/api/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        String tenantId = request.tenantId() == null || request.tenantId().isBlank() ? "tenant_a" : request.tenantId();
        return kernel.handle(tenantId, request.question(), request.sessionId());
    }

    @GetMapping("/api/sessions/{sessionId}")
    public QuerySession session(@PathVariable String sessionId) {
        return sessionStore.find(sessionId).orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));
    }

    public record QueryRequest(String question, String tenantId, String sessionId) {}
}
