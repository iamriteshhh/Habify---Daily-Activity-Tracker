package com.habify.dto;

import java.time.LocalDate;

public class DailyProgressDTO {

    private LocalDate date;
    private String dayOfWeek;
    private int totalScheduled;
    private int completed;
    private int partial;
    private int missed;
    private int completionPercentage;

    public DailyProgressDTO() {
    }

    public DailyProgressDTO(LocalDate date, String dayOfWeek, int totalScheduled, int completed, int partial, int missed, int completionPercentage) {
        this.date = date;
        this.dayOfWeek = dayOfWeek;
        this.totalScheduled = totalScheduled;
        this.completed = completed;
        this.partial = partial;
        this.missed = missed;
        this.completionPercentage = completionPercentage;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getTotalScheduled() {
        return totalScheduled;
    }

    public void setTotalScheduled(int totalScheduled) {
        this.totalScheduled = totalScheduled;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getPartial() {
        return partial;
    }

    public void setPartial(int partial) {
        this.partial = partial;
    }

    public int getMissed() {
        return missed;
    }

    public void setMissed(int missed) {
        this.missed = missed;
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(int completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
}
