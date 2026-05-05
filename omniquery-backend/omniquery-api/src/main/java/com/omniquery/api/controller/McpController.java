package com.omniquery.api.controller;

import com.omniquery.api.mcp.McpToolService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final McpToolService toolService;

    public McpController(McpToolService toolService) {
        this.toolService = toolService;
    }

    @PostMapping
    public Map<String, Object> handle(@RequestBody McpRequest request) {
        try {
            Object result = switch (request.method()) {
                case "tools/list" -> toolService.listTools();
                case "tools/call" -> {
                    McpToolCall call = McpToolCall.from(request.params());
                    yield toolService.call(call.name(), call.arguments());
                }
                default -> throw new McpToolService.UnknownToolException("Unknown method: " + request.method());
            };
            return Map.of("jsonrpc", "2.0", "id", request.id(), "result", result);
        } catch (McpToolService.UnknownToolException | McpToolService.InvalidToolArgumentsException e) {
            return Map.of(
                "jsonrpc", "2.0",
                "id", request.id(),
                "error", Map.of("code", -32602, "message", e.getMessage())
            );
        } catch (Exception e) {
            return Map.of(
                "jsonrpc", "2.0",
                "id", request.id(),
                "error", Map.of("code", -32603, "message", e.getMessage())
            );
        }
    }

    public record McpRequest(String jsonrpc, String id, String method, Map<String, Object> params) {}

    private record McpToolCall(String name, Map<String, Object> arguments) {
        @SuppressWarnings("unchecked")
        static McpToolCall from(Map<String, Object> params) {
            if (params == null) {
                throw new McpToolService.InvalidToolArgumentsException("Missing params");
            }
            Object name = params.get("name");
            if (name == null || name.toString().isBlank()) {
                throw new McpToolService.InvalidToolArgumentsException("Missing tool name");
            }
            Object arguments = params.get("arguments");
            Map<String, Object> argumentMap = arguments instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
            return new McpToolCall(name.toString(), argumentMap);
        }
    }
}
