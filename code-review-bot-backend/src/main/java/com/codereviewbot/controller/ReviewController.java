package com.codereviewbot.controller;

import com.codereviewbot.common.ApiResponse;
import com.codereviewbot.common.RateLimit;
import com.codereviewbot.dto.ReviewRequest;
import com.codereviewbot.dto.ReviewTaskResponse;
import com.codereviewbot.entity.ReviewIssue;
import com.codereviewbot.entity.ReviewTask;
import com.codereviewbot.service.ReviewAsyncService;
import com.codereviewbot.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewAsyncService asyncService;

    public ReviewController(ReviewService reviewService, ReviewAsyncService asyncService) {
        this.reviewService = reviewService;
        this.asyncService = asyncService;
    }

    // ── Async submit + poll ────────────────────────────────────────────────

    /**
     * Submit code review asynchronously. Returns taskId immediately.
     * Rate limited: 5 requests/min per user (enforced by RateLimitAspect via annotation).
     */
    @RateLimit(permits = 5, windowSeconds = 60, message = "请求过于频繁，每分钟最多 5 次审查")
    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submit(@Valid @RequestBody ReviewRequest request,
                                                    HttpServletRequest httpReq) {
        String userId = (String) httpReq.getAttribute("userId");
        ReviewTask task = asyncService.submit(userId, request.getCode(), request.getMode());
        return ApiResponse.ok(Map.of("taskId", task.getTaskId(), "status", task.getStatus()));
    }

    /** Poll async task status. */
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<ReviewTaskResponse> getTask(@PathVariable String taskId) {
        ReviewTask task = asyncService.getTask(taskId);
        if (task == null) {
            throw new ResponseStatusException(NOT_FOUND, "任务不存在");
        }
        return ApiResponse.ok(ReviewTaskResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .errorMessage(task.getErrorMessage())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build());
    }

    /**
     * Get completed review issues for a task.
     * Supports pagination via optional page/size query params.
     */
    @GetMapping("/tasks/{taskId}/issues")
    public ApiResponse<List<ReviewIssue>> getIssues(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(asyncService.getIssues(taskId, page, size));
    }

    // ── SSE streaming (real-time preview, kept for compatibility) ──────────

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter reviewStream(@Valid @RequestBody ReviewRequest request) {
        return reviewService.reviewStream(request);
    }
}
