package com.codereviewbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Shared DeepSeek API client — replaces duplicated HttpURLConnection code
 * that previously lived in both ReviewServiceImpl and ReviewAsyncService.
 *
 * Uses Spring RestClient (available since Spring Boot 3.2) for declarative,
 * pooled, timeout-aware HTTP calls.
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private RestClient restClient;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.model}")
    private String model;

    @PostConstruct
    void init() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "DEEPSEEK_API_KEY is not set. Configure it via environment variable or application-dev.yml");
        }
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        log.info("DeepSeekClient initialized — apiUrl={}, model={}", apiUrl, model);
    }

    /**
     * Send a non-streaming chat completion request. Returns raw content string.
     */
    public String chat(String systemPrompt, String userPrompt) {
        String requestBody = buildRequestBody(systemPrompt, userPrompt, false);

        String response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            return root.get("choices").get(0).get("message").get("content").asText();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse DeepSeek response: {}", response.substring(0, Math.min(200, response.length())));
            throw new RuntimeException("AI 服务返回格式异常", e);
        }
    }

    /**
     * Send a streaming chat completion request.
     * Returns a RestClient response that can be read line-by-line via SSE.
     */
    public RestClient.ResponseSpec chatStream(String systemPrompt, String userPrompt) {
        String requestBody = buildRequestBody(systemPrompt, userPrompt, true);

        return restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve();
    }

    private String buildRequestBody(String systemPrompt, String userPrompt, boolean stream) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", stream);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
        body.put("temperature", 0.3);
        body.put("max_tokens", 8192);

        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build request body", e);
        }
    }
}
