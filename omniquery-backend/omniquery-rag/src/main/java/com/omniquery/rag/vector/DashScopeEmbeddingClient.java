package com.omniquery.rag.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class DashScopeEmbeddingClient implements TextEmbeddingClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int dimensions;

    public DashScopeEmbeddingClient(String apiKey, String baseUrl, String model, int dimensions) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.dimensions = dimensions;
    }

    @Override
    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("MODEL_API_KEY is required for vector RAG embeddings");
        }
        try {
            String requestJson = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "input", text,
                "dimensions", dimensions,
                "encoding_format", "float"
            ));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/embeddings"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Embedding request failed with status " + response.statusCode() + ": " + response.body());
            }
            JsonNode embedding = objectMapper.readTree(response.body()).path("data").path(0).path("embedding");
            if (!embedding.isArray()) {
                throw new IllegalStateException("Embedding response did not contain data[0].embedding");
            }
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }
            return vector;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse embedding response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding request interrupted", ex);
        }
    }
}
