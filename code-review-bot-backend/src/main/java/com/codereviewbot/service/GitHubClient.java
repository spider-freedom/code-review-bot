package com.codereviewbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Minimal GitHub API client for fetching PR diffs and posting review comments.
 */
@Component
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String GITHUB_API = "https://api.github.com";

    private final RestClient restClient;

    @Value("${github.token:}")
    private String githubToken;

    public GitHubClient() {
        this.restClient = RestClient.builder()
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    public boolean isConfigured() {
        return githubToken != null && !githubToken.isBlank();
    }

    /**
     * Fetch the diff for a pull request.
     */
    public String getPullRequestDiff(String owner, String repo, int prNumber) {
        String url = String.format("%s/repos/%s/%s/pulls/%d", GITHUB_API, owner, repo, prNumber);

        String response = restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3.diff")
                .retrieve()
                .body(String.class);

        log.info("Fetched PR diff: {}/{}#{} — {} chars", owner, repo, prNumber, response.length());
        return response;
    }

    /**
     * Post a comment on a pull request.
     */
    public void postPullRequestComment(String owner, String repo, int prNumber, String body) {
        String url = String.format("%s/repos/%s/%s/issues/%d/comments", GITHUB_API, owner, repo, prNumber);

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("body", body);

        try {
            restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + githubToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(payload))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Posted review comment on {}/{}#{}", owner, repo, prNumber);
        } catch (Exception e) {
            log.error("Failed to post PR comment: {}", e.getMessage());
        }
    }
}
