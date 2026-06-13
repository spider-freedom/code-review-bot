package com.codereviewbot.controller;

import com.codereviewbot.dto.ReviewRequest;
import com.codereviewbot.dto.ReviewTaskResponse;
import com.codereviewbot.entity.ReviewIssue;
import com.codereviewbot.entity.ReviewTask;
import com.codereviewbot.service.ReviewAsyncService;
import com.codereviewbot.service.ReviewService;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewAsyncService asyncService;

    // Per-user rate limiters: 5 requests/minute per user
    private final ConcurrentHashMap<String, RateLimiter> userLimiters = new ConcurrentHashMap<>();

    public ReviewController(ReviewService reviewService, ReviewAsyncService asyncService) {
        this.reviewService = reviewService;
        this.asyncService = asyncService;
    }

    /**
     * Async submit — returns taskId immediately.
     * Rate limited: 5 requests/min per user.
     */
    @PostMapping("/submit")
    public Map<String, Object> submit(@Valid @RequestBody ReviewRequest request,
                                       HttpServletRequest httpReq) {
        String userId = (String) httpReq.getAttribute("userId");
        String code = request.getCode();
        String mode = request.getMode();

        // Rate limit check
        RateLimiter limiter = userLimiters.computeIfAbsent(userId,
                k -> RateLimiter.create(5.0 / 60.0));
        if (!limiter.tryAcquire(1, TimeUnit.SECONDS)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "请求过于频繁，每分钟最多 5 次审查");
        }

        ReviewTask task = asyncService.submit(userId, code, mode);
        return Map.of("taskId", task.getTaskId(), "status", task.getStatus());
    }

    /**
     * Poll task status.
     */
    @GetMapping("/tasks/{taskId}")
    public ReviewTaskResponse getTask(@PathVariable String taskId) {
        ReviewTask task = asyncService.getTask(taskId);
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        return ReviewTaskResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .errorMessage(task.getErrorMessage())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }

    /**
     * Get completed review issues.
     */
    @GetMapping("/tasks/{taskId}/issues")
    public List<ReviewIssue> getIssues(@PathVariable String taskId) {
        return asyncService.getIssues(taskId);
    }

    /**
     * Legacy SSE streaming endpoint — kept for real-time preview mode.
     * Not rate-limited (it's self-limiting due to connection overhead).
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter reviewStream(@Valid @RequestBody ReviewRequest request) {
        return reviewService.reviewStream(request);
    }
}
