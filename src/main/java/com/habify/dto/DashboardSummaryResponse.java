package com.habify.dto;

import java.time.LocalDate;
import java.util.List;

public class DashboardSummaryResponse {

    private LocalDate todayDate;
    private long totalHabits;
    private int scheduledTodayCount;
    private int completedTodayCount;
    private int partialTodayCount;
    private int missedTodayCount;
    private int todayCompletionPercentage;
    private int disciplineScore;
    private int bestCurrentStreak;
    private List<DailyProgressDTO> weeklyProgress;
    private List<TodayHabitDTO> todayHabits;
    private List<CategoryProgressDTO> categoryDistribution;
    private List<InsightDTO> insights;
    private String bestDay;
    private String weakestDay;
    private int weeklyAveragePercentage;

    public DashboardSummaryResponse() {
    }

    public List<CategoryProgressDTO> getCategoryDistribution() {
        return categoryDistribution;
    }

    public void setCategoryDistribution(List<CategoryProgressDTO> categoryDistribution) {
        this.categoryDistribution = categoryDistribution;
    }

    public List<InsightDTO> getInsights() {
        return insights;
    }

    public void setInsights(List<InsightDTO> insights) {
        this.insights = insights;
    }

    public String getBestDay() {
        return bestDay;
    }

    public void setBestDay(String bestDay) {
        this.bestDay = bestDay;
    }

    public String getWeakestDay() {
        return weakestDay;
    }

    public void setWeakestDay(String weakestDay) {
        this.weakestDay = weakestDay;
    }

    public int getWeeklyAveragePercentage() {
        return weeklyAveragePercentage;
    }

    public void setWeeklyAveragePercentage(int weeklyAveragePercentage) {
        this.weeklyAveragePercentage = weeklyAveragePercentage;
    }

    public LocalDate getTodayDate() {
        return todayDate;
    }

    public void setTodayDate(LocalDate todayDate) {
        this.todayDate = todayDate;
    }

    public long getTotalHabits() {
        return totalHabits;
    }

    public void setTotalHabits(long totalHabits) {
        this.totalHabits = totalHabits;
    }

    public int getScheduledTodayCount() {
        return scheduledTodayCount;
    }

    public void setScheduledTodayCount(int scheduledTodayCount) {
        this.scheduledTodayCount = scheduledTodayCount;
    }

    public int getCompletedTodayCount() {
        return completedTodayCount;
    }

    public void setCompletedTodayCount(int completedTodayCount) {
        this.completedTodayCount = completedTodayCount;
    }

    public int getPartialTodayCount() {
        return partialTodayCount;
    }

    public void setPartialTodayCount(int partialTodayCount) {
        this.partialTodayCount = partialTodayCount;
    }

    public int getMissedTodayCount() {
        return missedTodayCount;
    }

    public void setMissedTodayCount(int missedTodayCount) {
        this.missedTodayCount = missedTodayCount;
    }

    public int getTodayCompletionPercentage() {
        return todayCompletionPercentage;
    }

    public void setTodayCompletionPercentage(int todayCompletionPercentage) {
        this.todayCompletionPercentage = todayCompletionPercentage;
    }

    public int getDisciplineScore() {
        return disciplineScore;
    }

    public void setDisciplineScore(int disciplineScore) {
        this.disciplineScore = disciplineScore;
    }

    public int getBestCurrentStreak() {
        return bestCurrentStreak;
    }

    public void setBestCurrentStreak(int bestCurrentStreak) {
        this.bestCurrentStreak = bestCurrentStreak;
    }

    public List<DailyProgressDTO> getWeeklyProgress() {
        return weeklyProgress;
    }

    public void setWeeklyProgress(List<DailyProgressDTO> weeklyProgress) {
        this.weeklyProgress = weeklyProgress;
    }

    public List<TodayHabitDTO> getTodayHabits() {
        return todayHabits;
    }

    public void setTodayHabits(List<TodayHabitDTO> todayHabits) {
        this.todayHabits = todayHabits;
    }
}
