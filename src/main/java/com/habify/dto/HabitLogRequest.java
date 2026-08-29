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

    @NotNull(message = "Habit status is required (COMPLETED, PARTIAL, MISSED)")
    private HabitStatus status;

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
