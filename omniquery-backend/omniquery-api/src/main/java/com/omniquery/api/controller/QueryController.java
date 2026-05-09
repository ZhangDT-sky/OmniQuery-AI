package com.omniquery.api.controller;

import com.omniquery.core.engine.OrchestratorKernel;
import com.omniquery.core.model.QueryResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final OrchestratorKernel kernel;

    public QueryController(OrchestratorKernel kernel) {
        this.kernel = kernel;
    }

    @PostMapping
    public QueryResponse query(@RequestBody QueryRequest request) {
        String tenantId = request.tenantId() == null || request.tenantId().isBlank() ? "tenant_a" : request.tenantId();
        return kernel.handle(tenantId, request.question(), request.sessionId());
    }

    public record QueryRequest(String question, String tenantId, String sessionId) {}
}
