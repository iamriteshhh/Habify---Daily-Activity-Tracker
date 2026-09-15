package com.habify.dto;

import java.time.LocalDate;
import java.util.List;

public class CalendarDayDTO {

    private LocalDate date;
    private int dayOfMonth;
    private String dayOfWeek;
    private int scheduledCount;
    private int completedCount;
    private int partialCount;
    private int missedCount;
    private int completionPercentage;
    private int intensityLevel; // 0: None, 1: 1-49%, 2: 50-79%, 3: 80-100%
    private List<HabitLogResponse> logs;

    public CalendarDayDTO() {
    }

    public CalendarDayDTO(LocalDate date, int dayOfMonth, String dayOfWeek, int scheduledCount,
                          int completedCount, int partialCount, int missedCount,
                          int completionPercentage, int intensityLevel, List<HabitLogResponse> logs) {
        this.date = date;
        this.dayOfMonth = dayOfMonth;
        this.dayOfWeek = dayOfWeek;
        this.scheduledCount = scheduledCount;
        this.completedCount = completedCount;
        this.partialCount = partialCount;
        this.missedCount = missedCount;
        this.completionPercentage = completionPercentage;
        this.intensityLevel = intensityLevel;
        this.logs = logs;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getScheduledCount() {
        return scheduledCount;
    }

    public void setScheduledCount(int scheduledCount) {
        this.scheduledCount = scheduledCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getPartialCount() {
        return partialCount;
    }

    public void setPartialCount(int partialCount) {
        this.partialCount = partialCount;
    }

    public int getMissedCount() {
        return missedCount;
    }

    public void setMissedCount(int missedCount) {
        this.missedCount = missedCount;
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(int completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public int getIntensityLevel() {
        return intensityLevel;
    }

    public void setIntensityLevel(int intensityLevel) {
        this.intensityLevel = intensityLevel;
    }

    public List<HabitLogResponse> getLogs() {
        return logs;
    }

    public void setLogs(List<HabitLogResponse> logs) {
        this.logs = logs;
    }
}
