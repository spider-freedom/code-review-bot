package com.codereviewbot.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;

/**
 * Simple multi-tenant isolation via X-User-Id header.
 *
 * In production, this would be replaced by JWT authentication.
 * For this project, it provides user_id based data isolation
 * without requiring full user registration.
 */
@Component
public class ApiKeyFilter implements Filter {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;

        // Extract or generate userId for tenant isolation
        String userId = httpReq.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            // Fallback: use IP hash as default tenant identifier
            userId = "ip:" + hash(httpReq.getRemoteAddr());
        }

        // Store in request attribute for downstream use
        httpReq.setAttribute("userId", userId);

        chain.doFilter(request, response);
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.substring(0, 8);
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
