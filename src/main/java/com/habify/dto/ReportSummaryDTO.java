package com.habify.dto;

import java.time.LocalDate;
import java.util.List;

public class ReportSummaryDTO {

    private String periodType; // "WEEKLY" or "MONTHLY"
    private String periodTitle;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalHabits;
    private int totalScheduledActivities;
    private int completedActivities;
    private int partialActivities;
    private int missedActivities;
    private int overallCompletionPercentage;
    private int disciplineScore;
    private String bestDay;
    private String weakestDay;
    private List<HabitPerformanceDTO> habitBreakdown;

    public ReportSummaryDTO() {
    }

    public ReportSummaryDTO(String periodType, String periodTitle, LocalDate startDate, LocalDate endDate,
                            int totalHabits, int totalScheduledActivities, int completedActivities,
                            int partialActivities, int missedActivities, int overallCompletionPercentage,
                            int disciplineScore, String bestDay, String weakestDay,
                            List<HabitPerformanceDTO> habitBreakdown) {
        this.periodType = periodType;
        this.periodTitle = periodTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalHabits = totalHabits;
        this.totalScheduledActivities = totalScheduledActivities;
        this.completedActivities = completedActivities;
        this.partialActivities = partialActivities;
        this.missedActivities = missedActivities;
        this.overallCompletionPercentage = overallCompletionPercentage;
        this.disciplineScore = disciplineScore;
        this.bestDay = bestDay;
        this.weakestDay = weakestDay;
        this.habitBreakdown = habitBreakdown;
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public String getPeriodTitle() {
        return periodTitle;
    }

    public void setPeriodTitle(String periodTitle) {
        this.periodTitle = periodTitle;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getTotalHabits() {
        return totalHabits;
    }

    public void setTotalHabits(int totalHabits) {
        this.totalHabits = totalHabits;
    }

    public int getTotalScheduledActivities() {
        return totalScheduledActivities;
    }

    public void setTotalScheduledActivities(int totalScheduledActivities) {
        this.totalScheduledActivities = totalScheduledActivities;
    }

    public int getCompletedActivities() {
        return completedActivities;
    }

    public void setCompletedActivities(int completedActivities) {
        this.completedActivities = completedActivities;
    }

    public int getPartialActivities() {
        return partialActivities;
    }

    public void setPartialActivities(int partialActivities) {
        this.partialActivities = partialActivities;
    }

    public int getMissedActivities() {
        return missedActivities;
    }

    public void setMissedActivities(int missedActivities) {
        this.missedActivities = missedActivities;
    }

    public int getOverallCompletionPercentage() {
        return overallCompletionPercentage;
    }

    public void setOverallCompletionPercentage(int overallCompletionPercentage) {
        this.overallCompletionPercentage = overallCompletionPercentage;
    }

    public int getDisciplineScore() {
        return disciplineScore;
    }

    public void setDisciplineScore(int disciplineScore) {
        this.disciplineScore = disciplineScore;
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

    public List<HabitPerformanceDTO> getHabitBreakdown() {
        return habitBreakdown;
    }

    public void setHabitBreakdown(List<HabitPerformanceDTO> habitBreakdown) {
        this.habitBreakdown = habitBreakdown;
    }
}
