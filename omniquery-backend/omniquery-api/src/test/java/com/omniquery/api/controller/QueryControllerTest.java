package com.omniquery.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QueryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void returnsSessionWithRecentTurns() throws Exception {
        String queryResponse = mockMvc.perform(post("/api/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "question": "show recent orders with customer names",
                      "tenantId": "tenant_a"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(queryResponse);
        String sessionId = root.path("sessionId").asText();

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId", is(sessionId)))
            .andExpect(jsonPath("$.turns[0].userQuestion", is("show recent orders with customer names")));
    }
}
