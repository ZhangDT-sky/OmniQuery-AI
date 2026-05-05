package com.omniquery.api.mcp;

import com.omniquery.core.engine.OrchestratorKernel;
import com.omniquery.rag.repository.KnowledgeBase;
import com.omniquery.rag.service.RetrievalService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class McpToolService {

    private final KnowledgeBase knowledgeBase;
    private final RetrievalService retrievalService;
    private final OrchestratorKernel kernel;

    public McpToolService(KnowledgeBase knowledgeBase, RetrievalService retrievalService, OrchestratorKernel kernel) {
        this.knowledgeBase = knowledgeBase;
        this.retrievalService = retrievalService;
        this.kernel = kernel;
    }

    public Map<String, Object> listTools() {
        return Map.of("tools", List.of(
            tool("list_tables", "List queryable tables."),
            tool("describe_table", "Describe columns, relationships, and roles for one table."),
            tool("retrieve_context", "Retrieve schema and example context for a question."),
            tool("safe_query", "Run a natural-language query through NL2SQL, SQL guard, ACL rewrite, and read-only execution.")
        ));
    }

    public Object call(String name, Map<String, Object> arguments) {
        return switch (name) {
            case "list_tables" -> listTables();
            case "describe_table" -> describeTable(required(arguments, "table"));
            case "retrieve_context" -> retrievalService.retrieve(required(arguments, "question"));
            case "safe_query" -> kernel.handle(optional(arguments, "tenantId", "tenant_a"), required(arguments, "question"));
            default -> throw new UnknownToolException("Unknown tool: " + name);
        };
    }

    private Map<String, String> tool(String name, String description) {
        return Map.of("name", name, "description", description, "readOnly", "true");
    }

    private Map<String, Object> listTables() {
        return Map.of("tables", knowledgeBase.schemas().stream().map(schema -> schema.tableName()).toList());
    }

    private Object describeTable(String table) {
        return knowledgeBase.schemas().stream()
            .filter(schema -> schema.tableName().equalsIgnoreCase(table))
            .findFirst()
            .orElseThrow(() -> new UnknownToolException("Unknown table: " + table));
    }

    private String required(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new InvalidToolArgumentsException("Missing required argument: " + key);
        }
        return value.toString();
    }

    private String optional(Map<String, Object> arguments, String key, String fallback) {
        Object value = arguments.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }

    public static class UnknownToolException extends RuntimeException {
        public UnknownToolException(String message) {
            super(message);
        }
    }

    public static class InvalidToolArgumentsException extends RuntimeException {
        public InvalidToolArgumentsException(String message) {
            super(message);
        }
    }
}
