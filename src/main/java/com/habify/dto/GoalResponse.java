package com.habify.dto;

import com.habify.entity.GoalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GoalResponse {

    private Long id;
    private String title;
    private String description;
    private String targetType;
    private Integer targetValue;
    private Integer currentValue;
    private int progressPercentage;
    private String unit;
    private LocalDate deadline;
    private GoalStatus status;
    private LocalDateTime createdAt;

    public GoalResponse() {
    }

    public GoalResponse(Long id, String title, String description, String targetType,
                        Integer targetValue, Integer currentValue, int progressPercentage,
                        String unit, LocalDate deadline, GoalStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.targetType = targetType;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.progressPercentage = progressPercentage;
        this.unit = unit;
        this.deadline = deadline;
        this.status = status;
        this.createdAt = createdAt;
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

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Integer targetValue) {
        this.targetValue = targetValue;
    }

    public Integer getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Integer currentValue) {
        this.currentValue = currentValue;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public void setStatus(GoalStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
