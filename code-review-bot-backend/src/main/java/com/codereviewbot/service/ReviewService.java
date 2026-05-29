package com.codereviewbot.service;

import com.codereviewbot.dto.ReviewRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ReviewService {
    SseEmitter reviewStream(ReviewRequest request);
}
