package com.codereviewbot.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Per-user rate limiting via Guava RateLimiter (token bucket).
 * Intercepts methods annotated with @RateLimit.
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final LoadingCache<String, RateLimiter> userLimiters = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<>() {
                @Override
                public RateLimiter load(String userId) {
                    // Warmup: allow 1-permit burst so first request always succeeds
                    return RateLimiter.create(5.0 / 60.0, java.time.Duration.ofSeconds(10));
                }
            });

    @Around("@annotation(rateLimit)")
    public Object check(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String userId = getUserId();
        if (userId == null) {
            return joinPoint.proceed(); // No auth — allow through
        }

        double permitsPerSecond = rateLimit.permits() / (double) rateLimit.windowSeconds();
        RateLimiter limiter;
        try {
            limiter = userLimiters.get(userId);
            // Only update rate if changed — setRate() clears accumulated permits
            if (Math.abs(limiter.getRate() - permitsPerSecond) > 0.001) {
                limiter.setRate(permitsPerSecond);
            }
        } catch (Exception e) {
            limiter = RateLimiter.create(permitsPerSecond);
        }

        if (!limiter.tryAcquire()) {
            log.warn("Rate limit exceeded: user={}, rate={}/s", userId, String.format("%.2f", permitsPerSecond));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, rateLimit.message());
        }

        return joinPoint.proceed();
    }

    private String getUserId() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String header = request.getHeader("X-User-Id");
                if (header != null && !header.isBlank()) return header;
                return "ip:" + request.getRemoteAddr().hashCode();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
