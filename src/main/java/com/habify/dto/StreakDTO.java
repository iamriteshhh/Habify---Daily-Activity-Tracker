package com.habify.dto;

public class StreakDTO {

    private Long habitId;
    private String habitTitle;
    private int currentStreak;
    private int longestStreak;
    private int totalCompletedDays;

    public StreakDTO() {
    }

    public StreakDTO(Long habitId, String habitTitle, int currentStreak, int longestStreak, int totalCompletedDays) {
        this.habitId = habitId;
        this.habitTitle = habitTitle;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.totalCompletedDays = totalCompletedDays;
    }

    public Long getHabitId() {
        return habitId;
    }

    public void setHabitId(Long habitId) {
        this.habitId = habitId;
    }

    public String getHabitTitle() {
        return habitTitle;
    }

    public void setHabitTitle(String habitTitle) {
        this.habitTitle = habitTitle;
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

    public int getTotalCompletedDays() {
        return totalCompletedDays;
    }

    public void setTotalCompletedDays(int totalCompletedDays) {
        this.totalCompletedDays = totalCompletedDays;
    }
}
