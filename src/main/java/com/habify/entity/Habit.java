package com.habify.entity;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Habit Entity
 * 
 * Represents a personal habit defined by a User.
 * Supports frequency scheduling (Daily vs Specific Days of the week).
 */
@Entity
@Table(name = "habits")
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 50)
    private String category = "Other";

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_type", nullable = false, length = 30)
    private FrequencyType frequencyType = FrequencyType.DAILY;

    /**
     * Comma-separated uppercase day names, e.g., "MONDAY,WEDNESDAY,FRIDAY"
     * Null or empty when frequencyType is DAILY.
     */
    @Column(name = "scheduled_days", length = 100)
    private String scheduledDays;

    @Column(name = "target_value", nullable = false)
    private Integer targetValue = 1;

    @Column(length = 30)
    private String unit = "times";

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_time", length = 20)
    private TimeOfDay preferredTime = TimeOfDay.ANYTIME;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Habit() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    /**
     * Determines whether this habit is scheduled to be performed on the given date.
     * Respects habit frequency type and active status.
     */
    public boolean isScheduledFor(LocalDate date) {
        if (!Boolean.TRUE.equals(this.isActive)) {
            return false;
        }
        if (this.frequencyType == FrequencyType.DAILY) {
            return true;
        }
        if (this.scheduledDays == null || this.scheduledDays.trim().isEmpty()) {
            return true;
        }
        DayOfWeek dow = date.getDayOfWeek();
        Set<String> days = Arrays.stream(this.scheduledDays.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        return days.contains(dow.name());
    }

    public Set<String> getScheduledDaysSet() {
        if (this.scheduledDays == null || this.scheduledDays.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(this.scheduledDays.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public FrequencyType getFrequencyType() {
        return frequencyType;
    }

    public void setFrequencyType(FrequencyType frequencyType) {
        this.frequencyType = frequencyType;
    }

    public String getScheduledDays() {
        return scheduledDays;
    }

    public void setScheduledDays(String scheduledDays) {
        this.scheduledDays = scheduledDays;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Integer targetValue) {
        this.targetValue = targetValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public TimeOfDay getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(TimeOfDay preferredTime) {
        this.preferredTime = preferredTime;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
