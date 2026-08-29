package com.habify.dto;

import com.habify.entity.FrequencyType;
import com.habify.entity.Priority;
import com.habify.entity.TimeOfDay;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class HabitRequest {

    @NotBlank(message = "Habit title is required")
    @Size(min = 2, max = 120, message = "Habit title must be between 2 and 120 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Category is required")
    private String category = "Other";

    private Priority priority = Priority.MEDIUM;

    @NotNull(message = "Frequency type is required")
    private FrequencyType frequencyType = FrequencyType.DAILY;

    /**
     * E.g. "MONDAY,WEDNESDAY,FRIDAY" (Required if frequencyType is SPECIFIC_DAYS)
     */
    private String scheduledDays;

    @Min(value = 1, message = "Target value must be at least 1")
    private Integer targetValue = 1;

    private String unit = "times";

    private TimeOfDay preferredTime = TimeOfDay.ANYTIME;

    public HabitRequest() {
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
}
