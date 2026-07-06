package com.codereviewbot.service;

import com.codereviewbot.entity.ReviewIssue;
import com.codereviewbot.entity.ReviewTask;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles GitHub webhook events — fetches PR diff, runs async review,
 * and posts results back as a PR comment.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final GitHubClient gitHubClient;
    private final ReviewAsyncService reviewAsyncService;

    public WebhookService(GitHubClient gitHubClient, ReviewAsyncService reviewAsyncService) {
        this.gitHubClient = gitHubClient;
        this.reviewAsyncService = reviewAsyncService;
    }

    /**
     * Process a pull_request webhook event.
     */
    public void handlePullRequestEvent(String action, JsonNode payload) {
        if (!"opened".equals(action) && !"synchronize".equals(action) && !"labeled".equals(action)) {
            log.debug("Ignoring PR action: {}", action);
            return;
        }

        // For labeled events, only trigger on specific labels
        if ("labeled".equals(action)) {
            String label = payload.path("label").path("name").asText("");
            if (!"review".equalsIgnoreCase(label) && !"code-review".equalsIgnoreCase(label)) {
                log.debug("Ignoring label: {}", label);
                return;
            }
        }

        JsonNode pr = payload.path("pull_request");
        int prNumber = pr.path("number").asInt();
        JsonNode repo = payload.path("repository");
        String owner = repo.path("owner").path("login").asText();
        String repoName = repo.path("name").asText();

        if (owner.isEmpty() || repoName.isEmpty() || prNumber == 0) {
            log.warn("Invalid webhook payload: missing owner/repo/number");
            return;
        }

        reviewPullRequest(owner, repoName, prNumber);
    }

    /**
     * Fetch PR diff, run async review, and post results when complete.
     */
    private void reviewPullRequest(String owner, String repo, int prNumber) {
        if (!gitHubClient.isConfigured()) {
            log.warn("GitHub token not configured — skipping PR review for {}/{}#{}", owner, repo, prNumber);
            return;
        }

        try {
            // 1. Fetch PR diff
            String diff = gitHubClient.getPullRequestDiff(owner, repo, prNumber);
            if (diff == null || diff.isBlank()) {
                gitHubClient.postPullRequestComment(owner, repo, prNumber,
                        "🤖 Code Review Bot: PR 不包含代码变更，跳过审查。");
                return;
            }

            // 2. Submit async review (uses thread pool, returns immediately)
            String userId = "github:" + owner;
            ReviewTask task = reviewAsyncService.submit(userId, diff, "diff");

            // 3. Post initial comment
            gitHubClient.postPullRequestComment(owner, repo, prNumber,
                    "🤖 **Code Review Bot** 开始审查...\n\n"
                    + "审查任务 ID: `" + task.getTaskId() + "`\n"
                    + "变更内容: " + diff.lines().count() + " 行\n\n"
                    + "审查完成后结果将更新在这里。");

            // 4. Poll for completion on a background thread
            Thread pollThread = new Thread(() -> pollAndPostResult(owner, repo, prNumber, task.getTaskId()),
                    "gh-review-poll-" + prNumber);
            pollThread.setDaemon(true);
            pollThread.start();

        } catch (Exception e) {
            log.error("Failed to review PR {}/{}#{}", owner, repo, prNumber, e);
            gitHubClient.postPullRequestComment(owner, repo, prNumber,
                    "🤖 Code Review Bot: 审查过程出错 — " + e.getMessage());
        }
    }

    private void pollAndPostResult(String owner, String repo, int prNumber, String taskId) {
        int maxAttempts = 60; // 2 min max
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            ReviewTask task = reviewAsyncService.getTask(taskId, "github:" + owner);
            if (task == null) continue;

            if ("COMPLETED".equals(task.getStatus())) {
                postReviewResult(owner, repo, prNumber, taskId);
                return;
            }
            if ("FAILED".equals(task.getStatus())) {
                gitHubClient.postPullRequestComment(owner, repo, prNumber,
                        "🤖 Code Review Bot: 审查失败 — " + task.getErrorMessage());
                return;
            }
        }
    }

    private void postReviewResult(String owner, String repo, int prNumber, String taskId) {
        List<ReviewIssue> issues = reviewAsyncService.getIssues(taskId, "github:" + owner, 1, 50);
        if (issues.isEmpty()) {
            gitHubClient.postPullRequestComment(owner, repo, prNumber,
                    "✅ **Code Review Bot 审查完成** — 未发现明显问题");
            return;
        }

        long errors = issues.stream().filter(i -> "error".equals(i.getSeverity())).count();
        long warnings = issues.stream().filter(i -> "warning".equals(i.getSeverity())).count();
        long infos = issues.stream().filter(i -> "info".equals(i.getSeverity())).count();

        StringBuilder comment = new StringBuilder();
        comment.append("## 🤖 Code Review Bot 审查完成\n\n");
        comment.append(String.format("**共发现 %d 个问题**（严重: %d, 建议: %d, 优化: %d）\n\n",
                issues.size(), errors, warnings, infos));

        // Top issues (up to 10)
        int shown = 0;
        for (ReviewIssue issue : issues) {
            if (shown >= 10) break;
            String emoji = "error".equals(issue.getSeverity()) ? "🔴"
                    : "warning".equals(issue.getSeverity()) ? "🟡" : "🔵";
            comment.append(String.format("### %s %s (第 %d 行)\n", emoji, issue.getTitle(), issue.getLine()));
            comment.append(issue.getDescription()).append("\n\n");
            if (issue.getSuggestion() != null && !issue.getSuggestion().isBlank()) {
                comment.append("**建议**: ").append(issue.getSuggestion()).append("\n\n");
            }
            if (issue.getCodeExample() != null && !issue.getCodeExample().isBlank()) {
                comment.append("```java\n").append(issue.getCodeExample()).append("\n```\n\n");
            }
            shown++;
        }

        if (issues.size() > 10) {
            comment.append(String.format("\n> 还有 %d 个问题未展示。", issues.size() - 10));
        }

        gitHubClient.postPullRequestComment(owner, repo, prNumber, comment.toString());
    }
}
