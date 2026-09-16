package com.habify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ResetPasswordRequest
 * 
 * Payload containing reset token and the new password.
 */
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required.")
    private String token;

    @NotBlank(message = "New password is required.")
    @jakarta.validation.constraints.Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$",
        message = "Password must be at least 8 characters long and contain both letters and numbers."
    )
    private String newPassword;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
