package com.codereviewbot.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate-limit annotation. Production should use Redis + Lua for distributed enforcement.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** Permits per time window. Default: 5 per minute per user. */
    double permits() default 5.0;

    /** Time window in seconds. Default: 60 (1 minute). */
    long windowSeconds() default 60;

    /** Message returned when limit exceeded. */
    String message() default "请求过于频繁，请稍后重试";
}
