package com.habify.dto;

public class CategoryProgressDTO {

    private String category;
    private int totalScheduled;
    private int totalCompleted;
    private int totalPartial;
    private int totalMissed;
    private int completionPercentage;

    public CategoryProgressDTO() {
    }

    public CategoryProgressDTO(String category, int totalScheduled, int totalCompleted, int totalPartial, int totalMissed, int completionPercentage) {
        this.category = category;
        this.totalScheduled = totalScheduled;
        this.totalCompleted = totalCompleted;
        this.totalPartial = totalPartial;
        this.totalMissed = totalMissed;
        this.completionPercentage = completionPercentage;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getTotalScheduled() {
        return totalScheduled;
    }

    public void setTotalScheduled(int totalScheduled) {
        this.totalScheduled = totalScheduled;
    }

    public int getTotalCompleted() {
        return totalCompleted;
    }

    public void setTotalCompleted(int totalCompleted) {
        this.totalCompleted = totalCompleted;
    }

    public int getTotalPartial() {
        return totalPartial;
    }

    public void setTotalPartial(int totalPartial) {
        this.totalPartial = totalPartial;
    }

    public int getTotalMissed() {
        return totalMissed;
    }

    public void setTotalMissed(int totalMissed) {
        this.totalMissed = totalMissed;
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(int completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
}
