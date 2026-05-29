package com.codereviewbot.controller;

import com.codereviewbot.dto.ReviewRequest;
import com.codereviewbot.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping(value = "/review", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter review(@Valid @RequestBody ReviewRequest request) {
        return reviewService.reviewStream(request);
    }
}
