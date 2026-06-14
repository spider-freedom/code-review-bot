package com.codereviewbot.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unified request logging — logs every controller call with method, URI, args, and elapsed time.
 * Flags slow requests (> 3s) at WARN level.
 */
@Aspect
@Component
public class WebLogAspect {

    private static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);
    private static final long SLOW_THRESHOLD_MS = 3000;

    @Pointcut("execution(* com.codereviewbot.controller..*(..))")
    public void controllerLayer() {}

    @Around("controllerLayer()")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        String uri = getRequestUri();
        String safeArgs = safeArgs(joinPoint.getArgs());

        log.info("[REQ] {} {} | args={}", method, uri, safeArgs);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            if (elapsed > SLOW_THRESHOLD_MS) {
                log.warn("[SLOW] {} {} | {}ms (threshold {}ms)", method, uri, elapsed, SLOW_THRESHOLD_MS);
            } else {
                log.info("[RESP] {} {} | {}ms", method, uri, elapsed);
            }
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[ERROR] {} {} | {}ms | {}: {}", method, uri, elapsed,
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    private String getRequestUri() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest r = attrs.getRequest();
                return r.getMethod() + " " + r.getRequestURI();
            }
        } catch (Exception ignored) {}
        return "N/A";
    }

    private String safeArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        return Arrays.stream(args)
                .map(a -> {
                    if (a instanceof HttpServletRequest) return "HttpServletRequest";
                    if (a instanceof org.springframework.web.multipart.MultipartFile f)
                        return "MultipartFile(" + f.getOriginalFilename() + ")";
                    String s = String.valueOf(a);
                    return s.length() > 200 ? s.substring(0, 200) + "..." : s;
                })
                .toList()
                .toString();
    }
}
