package com.habify.dto;

public class HabitPerformanceDTO {

    private Long habitId;
    private String title;
    private String category;
    private int scheduledCount;
    private int completedCount;
    private int partialCount;
    private int missedCount;
    private int completionPercentage;
    private int currentStreak;

    public HabitPerformanceDTO() {
    }

    public HabitPerformanceDTO(Long habitId, String title, String category, int scheduledCount,
                               int completedCount, int partialCount, int missedCount,
                               int completionPercentage, int currentStreak) {
        this.habitId = habitId;
        this.title = title;
        this.category = category;
        this.scheduledCount = scheduledCount;
        this.completedCount = completedCount;
        this.partialCount = partialCount;
        this.missedCount = missedCount;
        this.completionPercentage = completionPercentage;
        this.currentStreak = currentStreak;
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

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }
}
