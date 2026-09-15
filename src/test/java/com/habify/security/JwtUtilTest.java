package com.habify.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    // 64-byte hex key for HMAC-SHA256
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, expirationMs);
    }

    @Test
    @DisplayName("Generate and validate valid JWT token")
    void testGenerateAndValidateToken() {
        String token = jwtUtil.generateToken(42L, "alex_dev", "alex@habify.com");

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(42L, jwtUtil.getUserIdFromToken(token));
        assertEquals("alex_dev", jwtUtil.getUsernameFromToken(token));
        assertEquals("alex@habify.com", jwtUtil.getEmailFromToken(token));
    }

    @Test
    @DisplayName("Reject expired token")
    void testExpiredToken() {
        // Expiration in the past (-1000 ms)
        JwtUtil expiredJwtUtil = new JwtUtil(secret, -1000L);
        String expiredToken = expiredJwtUtil.generateToken(1L, "expired_user", "expired@habify.com");

        assertFalse(expiredJwtUtil.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Reject tampered token")
    void testTamperedToken() {
        String token = jwtUtil.generateToken(1L, "legit_user", "legit@habify.com");
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertFalse(jwtUtil.validateToken(tamperedToken));
    }
}
