package com.habify.dto;

import com.habify.entity.HabitStatus;
import com.habify.entity.Priority;
import com.habify.entity.TimeOfDay;

public class TodayHabitDTO {

    private Long habitId;
    private String title;
    private String category;
    private Priority priority;
    private TimeOfDay preferredTime;
    private Integer targetValue;
    private String unit;
    private boolean isScheduledToday;
    private HabitStatus status;
    private Integer completionPercentage;
    private Double actualValue;
    private Long logId;
    private String note;
    private int currentStreak;

    public TodayHabitDTO() {
    }

    public Long getHabitId() {
        return habitId;
    }

    public void setHabitId(Long habitId) {
        this.habitId = habitId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public TimeOfDay getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(TimeOfDay preferredTime) {
        this.preferredTime = preferredTime;
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

    public boolean isScheduledToday() {
        return isScheduledToday;
    }

    public void setScheduledToday(boolean scheduledToday) {
        isScheduledToday = scheduledToday;
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

    public Double getActualValue() {
        return actualValue;
    }

    public void setActualValue(Double actualValue) {
        this.actualValue = actualValue;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }
}
