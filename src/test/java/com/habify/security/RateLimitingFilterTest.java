package com.habify.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitingFilterTest {

    @Test
    @DisplayName("Allows requests under rate limit and blocks requests exceeding limit")
    void testRateLimitingEnforcement() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();

        // Send 15 login requests from IP 192.168.1.50 (should all pass)
        for (int i = 0; i < 15; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("192.168.1.50");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus(), "Request " + (i + 1) + " should be permitted");
        }

        // 16th request should be blocked with 429 Too Many Requests
        MockHttpServletRequest request16 = new MockHttpServletRequest("POST", "/api/auth/login");
        request16.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response16 = new MockHttpServletResponse();
        filter.doFilter(request16, response16, new MockFilterChain());
        assertEquals(429, response16.getStatus(), "Request 16 should be blocked with HTTP 429");

        // Non-rate-limited path should not be blocked
        MockHttpServletRequest getHabits = new MockHttpServletRequest("GET", "/api/habits");
        getHabits.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse getResponse = new MockHttpServletResponse();
        filter.doFilter(getHabits, getResponse, new MockFilterChain());
        assertEquals(200, getResponse.getStatus(), "GET /api/habits should not be rate-limited");
    }

    @Test
    @DisplayName("Anti-Spoofing: Rotating X-Forwarded-For headers cannot bypass rate limit when trustProxy is false")
    void testSpoofedHeaderCannotBypassRateLimit() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(false);

        // Attacker attempts 15 logins using rotating spoofed X-Forwarded-For headers from same IP
        for (int i = 0; i < 15; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("203.0.113.195");
            request.addHeader("X-Forwarded-For", "198.51.100." + i);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus(), "Request " + (i + 1) + " should be permitted");
        }

        // 16th login attempt with yet another spoofed IP must be BLOCKED
        MockHttpServletRequest request16 = new MockHttpServletRequest("POST", "/api/auth/login");
        request16.setRemoteAddr("203.0.113.195");
        request16.addHeader("X-Forwarded-For", "198.51.100.254");
        MockHttpServletResponse response16 = new MockHttpServletResponse();
        filter.doFilter(request16, response16, new MockFilterChain());
        assertEquals(429, response16.getStatus(), "Spoofed header request 16 must be blocked with HTTP 429");
    }

    @Test
    @DisplayName("Trusted Proxy: Correctly uses X-Forwarded-For when trustProxy is explicitly enabled")
    void testTrustedProxyRespectsForwardedHeader() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(true);

        // Legitimate client behind reverse proxy
        for (int i = 0; i < 15; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("127.0.0.1"); // Reverse proxy address
            request.addHeader("X-Forwarded-For", "198.51.100.50");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }

        // 16th request from same forwarded client IP is blocked
        MockHttpServletRequest request16 = new MockHttpServletRequest("POST", "/api/auth/login");
        request16.setRemoteAddr("127.0.0.1");
        request16.addHeader("X-Forwarded-For", "198.51.100.50");
        MockHttpServletResponse response16 = new MockHttpServletResponse();
        filter.doFilter(request16, response16, new MockFilterChain());
        assertEquals(429, response16.getStatus());

        // A different client IP through the same reverse proxy is permitted
        MockHttpServletRequest differentClient = new MockHttpServletRequest("POST", "/api/auth/login");
        differentClient.setRemoteAddr("127.0.0.1");
        differentClient.addHeader("X-Forwarded-For", "198.51.100.99");
        MockHttpServletResponse diffResponse = new MockHttpServletResponse();
        filter.doFilter(differentClient, diffResponse, new MockFilterChain());
        assertEquals(200, diffResponse.getStatus(), "Different client behind proxy should be permitted");
    }
}
