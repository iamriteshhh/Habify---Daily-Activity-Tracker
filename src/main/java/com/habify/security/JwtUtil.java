package com.habify.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JwtUtil
 * 
 * Generates and validates cryptographic HMAC-SHA256 JWT tokens.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${habify.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
                   @Value("${habify.jwt.expiration-ms:86400000}") long expirationMs) {
        byte[] keyBytes;
        String cleanSecret = secret != null ? secret.trim() : "";
        if (cleanSecret.matches("^[0-9a-fA-F]{64}$")) {
            // Hex-encoded 256-bit key
            keyBytes = java.util.HexFormat.of().parseHex(cleanSecret);
        } else {
            keyBytes = cleanSecret.getBytes(StandardCharsets.UTF_8);
        }

        // Safety check: ensure at least 256 bits (32 bytes)
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT with user details and token version.
     */
    public String generateToken(Long userId, String username, String email, Integer tokenVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("email", email)
                .claim("tokenVersion", tokenVersion != null ? tokenVersion : 1)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Backward-compatible token generator.
     */
    public String generateToken(Long userId, String username, String email) {
        return generateToken(userId, username, email, 1);
    }

    /**
     * Extracts all claims from a JWT string.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts user ID from token.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object idObj = claims.get("userId");
        if (idObj instanceof Number) {
            return ((Number) idObj).longValue();
        }
        return Long.parseLong(idObj.toString());
    }

    /**
     * Extracts username from token.
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * Extracts email from token.
     */
    public String getEmailFromToken(String token) {
        return parseToken(token).get("email", String.class);
    }

    /**
     * Extracts token version from token.
     */
    public Integer getTokenVersionFromToken(String token) {
        Claims claims = parseToken(token);
        Object tv = claims.get("tokenVersion");
        if (tv instanceof Number) {
            return ((Number) tv).intValue();
        }
        if (tv != null) {
            try {
                return Integer.parseInt(tv.toString());
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    /**
     * Validates if the token is properly signed and unexpired.
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
