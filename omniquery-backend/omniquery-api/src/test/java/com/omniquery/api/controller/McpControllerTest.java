package com.omniquery.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class McpControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void listsReadOnlyTools() throws Exception {
        mockMvc.perform(post("/api/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "jsonrpc": "2.0",
                      "id": "1",
                      "method": "tools/list",
                      "params": {}
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jsonrpc", is("2.0")))
            .andExpect(jsonPath("$.id", is("1")))
            .andExpect(jsonPath("$.result.tools[*].name", hasItem("safe_query")))
            .andExpect(jsonPath("$.result.tools[*].name", hasItem("list_tables")))
            .andExpect(jsonPath("$.result.tools[*].name", hasItem("describe_table")))
            .andExpect(jsonPath("$.result.tools[*].name", hasItem("retrieve_context")));
    }

    @Test
    void returnsJsonRpcErrorForUnknownTool() throws Exception {
        mockMvc.perform(post("/api/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "jsonrpc": "2.0",
                      "id": "2",
                      "method": "tools/call",
                      "params": {
                        "name": "drop_everything",
                        "arguments": {}
                      }
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error.code", is(-32602)))
            .andExpect(jsonPath("$.error.message", is("Unknown tool: drop_everything")));
    }

    @Test
    void safeQueryRunsThroughGuardAclAndExecution() throws Exception {
        String response = mockMvc.perform(post("/api/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "jsonrpc": "2.0",
                      "id": "3",
                      "method": "tools/call",
                      "params": {
                        "name": "safe_query",
                        "arguments": {
                          "question": "show total paid amount by customer",
                          "tenantId": "tenant_a"
                        }
                      }
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.success", is(true)))
            .andExpect(jsonPath("$.result.guardedSql").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        String guardedSql = root.path("result").path("guardedSql").asText().toLowerCase();
        String rows = root.path("result").path("rows").toString();
        org.junit.jupiter.api.Assertions.assertTrue(guardedSql.contains("tenant_id"));
        org.junit.jupiter.api.Assertions.assertFalse(rows.contains("9999"));
    }
}
