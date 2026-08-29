package com.habify.dto;

import java.time.LocalDateTime;

public class UserProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private String username;
    private long totalHabits;
    private long activeHabits;
    private LocalDateTime createdAt;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long id, String fullName, String email, String username, long totalHabits, long activeHabits, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.username = username;
        this.totalHabits = totalHabits;
        this.activeHabits = activeHabits;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public long getTotalHabits() {
        return totalHabits;
    }

    public void setTotalHabits(long totalHabits) {
        this.totalHabits = totalHabits;
    }

    public long getActiveHabits() {
        return activeHabits;
    }

    public void setActiveHabits(long activeHabits) {
        this.activeHabits = activeHabits;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
