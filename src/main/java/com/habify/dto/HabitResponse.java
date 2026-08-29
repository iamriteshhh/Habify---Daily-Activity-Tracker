package com.habify.dto;

import com.habify.entity.FrequencyType;
import com.habify.entity.HabitStatus;
import com.habify.entity.Priority;
import com.habify.entity.TimeOfDay;

import java.time.LocalDateTime;
import java.util.Set;

public class HabitResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private Priority priority;
    private FrequencyType frequencyType;
    private String scheduledDays;
    private Set<String> scheduledDaysSet;
    private Integer targetValue;
    private String unit;
    private TimeOfDay preferredTime;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    // Dynamic analytics fields
    private int currentStreak;
    private int longestStreak;
    private HabitStatus todayStatus;
    private Integer todayPercentage;
    private boolean isScheduledToday;

    public HabitResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Set<String> getScheduledDaysSet() {
        return scheduledDaysSet;
    }

    public void setScheduledDaysSet(Set<String> scheduledDaysSet) {
        this.scheduledDaysSet = scheduledDaysSet;
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

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public HabitStatus getTodayStatus() {
        return todayStatus;
    }

    public void setTodayStatus(HabitStatus todayStatus) {
        this.todayStatus = todayStatus;
    }

    public Integer getTodayPercentage() {
        return todayPercentage;
    }

    public void setTodayPercentage(Integer todayPercentage) {
        this.todayPercentage = todayPercentage;
    }

    public boolean isScheduledToday() {
        return isScheduledToday;
    }

    public void setScheduledToday(boolean scheduledToday) {
        isScheduledToday = scheduledToday;
    }
}
