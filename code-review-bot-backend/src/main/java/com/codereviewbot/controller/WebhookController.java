package com.codereviewbot.controller;

import com.codereviewbot.common.ApiResponse;
import com.codereviewbot.service.WebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives GitHub webhook events and dispatches to WebhookService.
 *
 * Supported events:
 * - pull_request (opened, synchronize, labeled)
 * - issue_comment (for /review slash command)
 */
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * GitHub webhook endpoint.
     *
     * GitHub sends POST requests with event type in X-GitHub-Event header.
     * Respond quickly (GitHub expects <10s) — processing is async.
     */
    @PostMapping(value = "/github", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<String> handleGitHubWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestBody String rawPayload) {

        log.info("Received GitHub webhook: event={}, payloadLength={}", event, rawPayload.length());

        try {
            JsonNode payload = objectMapper.readTree(rawPayload);

            if ("pull_request".equals(event)) {
                String action = payload.path("action").asText();
                webhookService.handlePullRequestEvent(action, payload);
                return ApiResponse.ok("PR review queued");
            }

            if ("issue_comment".equals(event)) {
                String commentBody = payload.path("comment").path("body").asText();
                if (commentBody != null && commentBody.trim().startsWith("/review")) {
                    // Treat comment-triggered review as a pull_request review
                    String action = "created";
                    // Enrich payload to simulate PR event structure
                    webhookService.handlePullRequestEvent(action, payload);
                    return ApiResponse.ok("Comment-triggered review queued");
                }
            }

            return ApiResponse.ok("event received");
        } catch (Exception e) {
            log.error("Failed to process webhook: {}", e.getMessage());
            return ApiResponse.ok("error: " + e.getMessage());
        }
    }
}
