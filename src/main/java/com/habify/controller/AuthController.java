package com.habify.controller;

import com.habify.config.AuthHelper;
import com.habify.dto.AuthResponse;
import com.habify.dto.LoginRequest;
import com.habify.dto.RegisterRequest;
import com.habify.dto.UserProfileResponse;
import com.habify.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController
 * 
 * Handles user registration, login, and profile retrieval.
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

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }
}
