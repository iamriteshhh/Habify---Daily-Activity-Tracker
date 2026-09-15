package com.habify.dto;

import com.habify.entity.HabitStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class HabitLogRequest {

    /**
     * Date of the log. Defaults to today's date if omitted.
     */
    private LocalDate logDate;

    /**
     * Optional explicit status. Auto-derived from actualValue/targetValue if omitted.
     */
    private HabitStatus status;

    /**
     * Actual progress value logged against habit's target (e.g. 1.5 hours out of 2).
     */
    @Min(value = 0, message = "Actual progress value cannot be negative")
    private Double actualValue;

    @Min(value = 0, message = "Completion percentage cannot be less than 0")
    @Max(value = 100, message = "Completion percentage cannot exceed 100")
    private Integer completionPercentage;

    @Size(max = 300, message = "Note cannot exceed 300 characters")
    private String note;

    public HabitLogRequest() {
    }

    public HabitLogRequest(LocalDate logDate, HabitStatus status, Integer completionPercentage, String note) {
        this.logDate = logDate;
        this.status = status;
        this.completionPercentage = completionPercentage;
        this.note = note;
    }

    public HabitLogRequest(LocalDate logDate, HabitStatus status, Double actualValue, Integer completionPercentage, String note) {
        this.logDate = logDate;
        this.status = status;
        this.actualValue = actualValue;
        this.completionPercentage = completionPercentage;
        this.note = note;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDate logDate) {
        this.logDate = logDate;
    }

    public HabitStatus getStatus() {
        return status;
    }

    public void setStatus(HabitStatus status) {
        this.status = status;
    }

    public Double getActualValue() {
        return actualValue;
    }

    public void setActualValue(Double actualValue) {
        this.actualValue = actualValue;
    }

    public Integer getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(Integer completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
