package com.habify.config;

import com.habify.exception.UnauthorizedException;
import com.habify.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AuthHelper
 * 
 * Secure identity resolver for Habify.
 * Resolves the authenticated User ID exclusively from the verified
 * Spring Security Context populated by the JWT filter.
 * 
 * Client-supplied headers (X-User-Id) and query parameters (?userId=)
 * are strictly forbidden and ignored to prevent user impersonation.
 */
@Component
public class AuthHelper {

    public Long getAuthenticatedUserId(HttpServletRequest request) {
        return getAuthenticatedUserId();
    }

    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }

        throw new UnauthorizedException("Authentication required. Please log in.");
    }

    public UserPrincipal getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }

        throw new UnauthorizedException("Authentication required. Please log in.");
    }

    public void requireEmailVerified() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        if (!principal.isEmailVerified()) {
            throw new UnauthorizedException("Email verification required. Please verify your email to access this resource.");
        }
    }
}
