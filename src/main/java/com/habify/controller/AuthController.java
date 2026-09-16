package com.habify.controller;

import com.habify.config.AuthHelper;
import com.habify.dto.*;
import com.habify.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController
 * 
 * Handles user authentication, registration, email verification,
 * password recovery, and authenticated profile retrieval.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthHelper authHelper;

    public AuthController(UserService userService, AuthHelper authHelper) {
        this.userService = userService;
        this.authHelper = authHelper;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam("token") String token) {
        MessageResponse response = userService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse response = userService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = userService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        userService.logout(userId);
        return ResponseEntity.ok(MessageResponse.ok("Logged out successfully. All sessions have been invalidated."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        MessageResponse response = userService.resendVerification(userId);
        return ResponseEntity.ok(response);
    }
}
