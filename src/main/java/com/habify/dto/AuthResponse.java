package com.habify.dto;

public class AuthResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String username;
    private String token;
    private String message;

    public AuthResponse() {
    }

    public AuthResponse(Long userId, String fullName, String email, String username, String token, String message) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.username = username;
        this.token = token;
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
