package com.habify.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RateLimitingFilter
 * 
 * Thread-safe IP-based rate limiter for authentication endpoints.
 * Mitigates credential stuffing, brute-force attacks, and automated spam.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 15;
    private static final long WINDOW_MS = 60_000L;

    private final boolean trustProxy;

    public RateLimitingFilter() {
        this(false);
    }

    public RateLimitingFilter(@org.springframework.beans.factory.annotation.Value("${habify.security.rate-limit.trust-proxy:false}") boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    private static class RequestCounter {
        final long windowStart;
        final AtomicInteger count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RequestCounter> clientCounters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Apply rate limit specifically to sensitive authentication endpoints
        if (isRateLimitedPath(path, request.getMethod())) {
            String clientIp = extractClientIp(request);
            long now = System.currentTimeMillis();

            RequestCounter counter = clientCounters.compute(clientIp, (key, existing) -> {
                if (existing == null || (now - existing.windowStart) > WINDOW_MS) {
                    return new RequestCounter(now);
                }
                existing.count.incrementAndGet();
                return existing;
            });

            if (counter.count.get() > MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(429); // 429 Too Many Requests
                response.setHeader("Retry-After", "60");
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many authentication attempts. Please wait 60 seconds before trying again.\"}");
                return;
            }

            // Periodic memory cleanup: evict expired IP counters if table grows
            if (clientCounters.size() > 5000) {
                clientCounters.entrySet().removeIf(e -> (now - e.getValue().windowStart) > WINDOW_MS);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String path, String method) {
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }
        return path.endsWith("/api/auth/login") ||
               path.endsWith("/api/auth/register") ||
               path.endsWith("/api/auth/forgot-password") ||
               path.endsWith("/api/auth/reset-password");
    }

    private String extractClientIp(HttpServletRequest request) {
        if (trustProxy) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
