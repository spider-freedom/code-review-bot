package com.codereviewbot.service;

import com.codereviewbot.entity.ReviewIssue;
import com.codereviewbot.entity.ReviewTask;
import com.codereviewbot.mapper.ReviewIssueMapper;
import com.codereviewbot.mapper.ReviewTaskMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

/**
 * Async code review service.
 *
 * Design decisions:
 * - Thread pool (3 workers) for AI calls. Production: message queue (Kafka/RocketMQ).
 * - Redis cache by MD5(code) — 1h TTL — prevents re-reviewing identical code.
 * - Task state persisted to review_task table for restart durability.
 * - DeepSeekClient encapsulates AI API calls (replaced raw HttpURLConnection).
 */
@Service
public class ReviewAsyncService {

    private static final Logger log = LoggerFactory.getLogger(ReviewAsyncService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "review-worker");
        t.setDaemon(true);
        return t;
    });

    private final ReviewTaskMapper taskMapper;
    private final ReviewIssueMapper issueMapper;
    private final StringRedisTemplate redisTemplate;
    private final DeepSeekClient deepSeekClient;

    public ReviewAsyncService(ReviewTaskMapper taskMapper,
                              ReviewIssueMapper issueMapper,
                              StringRedisTemplate redisTemplate,
                              DeepSeekClient deepSeekClient) {
        this.taskMapper = taskMapper;
        this.issueMapper = issueMapper;
        this.redisTemplate = redisTemplate;
        this.deepSeekClient = deepSeekClient;
    }

    @PreDestroy
    void shutdown() {
        log.info("Shutting down review executor pool");
        executor.shutdown();
    }

    /** Submit review task — returns immediately. */
    public ReviewTask submit(String userId, String code, String mode) {
        String codeHash = md5(code);
        ReviewTask task = new ReviewTask();
        task.setUserId(userId);
        task.setCodeHash(codeHash);
        task.setStatus("PENDING");
        task.setMode(mode);
        task.setCode(code.length() > 10000 ? code.substring(0, 10000) : code);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);

        executor.submit(() -> processTask(task));

        return task;
    }

    /**
     * Query task by ID, scoped to user for tenant isolation.
     */
    public ReviewTask getTask(String taskId, String userId) {
        return taskMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewTask>()
                        .eq(ReviewTask::getTaskId, taskId)
                        .eq(ReviewTask::getUserId, userId));
    }

    /**
     * Query issues for a completed task, scoped to user with pagination.
     */
    public List<ReviewIssue> getIssues(String taskId, String userId, int page, int size) {
        ReviewTask task = taskMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewTask>()
                        .eq(ReviewTask::getTaskId, taskId)
                        .eq(ReviewTask::getUserId, userId));
        if (task == null) {
            return List.of();
        }
        return issueMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewIssue>()
                        .eq(ReviewIssue::getTaskId, taskId)
                        .orderByAsc(ReviewIssue::getSeverity)
        ).getRecords();
    }

    // ── Processing ──────────────────────────────────────────────────────────

    private void processTask(ReviewTask task) {
        log.info("Processing review task {} (user={})", task.getTaskId(), task.getUserId());
        task.setStatus("PROCESSING");
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        try {
            String cacheKey = "review:result:" + task.getCodeHash();
            List<ReviewIssue> issues;
            boolean cacheHit = false;

            // 1. Check Redis cache (gracefully degrade if Redis unavailable)
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null) {
                    log.info("Cache hit for task {}", task.getTaskId());
                    issues = parseIssuesFromJson(cachedJson);
                    cacheHit = true;
                } else {
                    issues = callAndParse(task);
                }
            } catch (Exception redisEx) {
                log.warn("Redis unavailable, falling back to API call: {}", redisEx.getMessage());
                issues = callAndParse(task);
            }

            // 2. Save results
            saveIssues(task.getTaskId(), issues);

            // 3. Try cache result (silently ignore if Redis unavailable)
            if (!cacheHit) {
                try {
                    String json = objectMapper.writeValueAsString(issues);
                    redisTemplate.opsForValue().set(cacheKey, json, Duration.ofHours(1));
                } catch (Exception redisEx) {
                    log.debug("Failed to cache result (Redis unavailable): {}", redisEx.getMessage());
                }
            }

            task.setStatus("COMPLETED");
            task.setErrorMessage(cacheHit ? "hit_cache" : "fresh");
            log.info("Task {} completed: {} issues ({})", task.getTaskId(), issues.size(), task.getErrorMessage());
        } catch (Exception e) {
            log.error("Task {} failed: {}", task.getTaskId(), e.getMessage());
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
        }

        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    // ── AI ──────────────────────────────────────────────────────────────────

    private List<ReviewIssue> callAndParse(ReviewTask task) {
        String contentDescription = "code".equals(task.getMode())
                ? "请审查以下代码：" : "请审查以下 git diff 内容：";

        String systemPrompt = """
            你是一个资深代码审查专家，请从以下维度分析：
            1. 潜在Bug 2. 安全漏洞 3. 性能问题 4. 代码规范
            严格返回 JSON 数组：[{"severity":"error|warning|info","line":数字,"title":"...","description":"...","suggestion":"...","codeExample":"..."}]
            没有问题则返回 []。不要包含 markdown 标记。""";

        String raw = deepSeekClient.chat(systemPrompt, contentDescription + "\n\n" + task.getCode());
        return parseIssuesFromJson(raw);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void saveIssues(String taskId, List<ReviewIssue> issues) {
        LocalDateTime now = LocalDateTime.now();
        issues.forEach(i -> i.setTaskId(taskId));
        issues.forEach(i -> i.setCreateTime(now));
        issues.forEach(i -> issueMapper.insert(i));
    }

    private List<ReviewIssue> parseIssuesFromJson(String raw) {
        List<ReviewIssue> issues = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(raw);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    issues.add(parseIssue(node));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse issues JSON: {}", e.getMessage());
        }
        return issues;
    }

    private ReviewIssue parseIssue(JsonNode node) {
        return ReviewIssue.builder()
                .severity(node.has("severity") ? node.get("severity").asText() : "info")
                .line(node.has("line") ? node.get("line").asInt() : 0)
                .title(node.has("title") ? node.get("title").asText() : "")
                .description(node.has("description") ? node.get("description").asText() : "")
                .suggestion(node.has("suggestion") ? node.get("suggestion").asText() : "")
                .codeExample(node.has("codeExample") ? node.get("codeExample").asText() : null)
                .build();
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
