package com.habify.config;

import com.habify.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * AuthHelper
 * 
 * Simple, student-friendly authentication helper.
 * Resolves the authenticated User ID from either the 'X-User-Id' header
 * or the 'Authorization: Bearer hbf_<userId>_<token>' header.
 */
@Component
public class AuthHelper {

    public Long getAuthenticatedUserId(HttpServletRequest request) {
        // 1. Check custom X-User-Id header
        String xUserId = request.getHeader("X-User-Id");
        if (xUserId != null && !xUserId.trim().isEmpty()) {
            try {
                return Long.parseLong(xUserId.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        // 2. Check Authorization Bearer token (format: hbf_{userId}_{uuid})
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (token.startsWith("hbf_")) {
                String[] parts = token.split("_");
                if (parts.length >= 2) {
                    try {
                        return Long.parseLong(parts[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // 3. Fallback to query parameter ?userId=
        String paramUserId = request.getParameter("userId");
        if (paramUserId != null && !paramUserId.trim().isEmpty()) {
            try {
                return Long.parseLong(paramUserId.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        throw new UnauthorizedException("Authentication required. Please log in.");
    }
}
