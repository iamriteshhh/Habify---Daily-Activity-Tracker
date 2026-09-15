package com.habify.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class GoalRequest {

    @NotBlank(message = "Goal title is required.")
    @Size(max = 120, message = "Goal title cannot exceed 120 characters.")
    private String title;

    @Size(max = 300, message = "Description cannot exceed 300 characters.")
    private String description;

    @NotNull(message = "Target value is required.")
    @Min(value = 1, message = "Target value must be at least 1.")
    private Integer targetValue;

    @Size(max = 30, message = "Unit cannot exceed 30 characters.")
    private String unit = "times";

    private LocalDate deadline;

    public GoalRequest() {
    }

    public GoalRequest(String title, String description, Integer targetValue, String unit, LocalDate deadline) {
        this.title = title;
        this.description = description;
        this.targetValue = targetValue;
        this.unit = unit;
        this.deadline = deadline;
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

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
