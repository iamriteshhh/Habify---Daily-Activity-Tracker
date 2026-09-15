package com.habify.dto;

public class AchievementDTO {

    private String id;
    private String title;
    private String description;
    private String icon;
    private boolean isUnlocked;
    private int currentProgress;
    private int targetProgress;
    private int progressPercentage;
    private String category;

    public AchievementDTO() {
    }

    public AchievementDTO(String id, String title, String description, String icon, boolean isUnlocked,
                          int currentProgress, int targetProgress, int progressPercentage, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.isUnlocked = isUnlocked;
        this.currentProgress = currentProgress;
        this.targetProgress = targetProgress;
        this.progressPercentage = progressPercentage;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }

    public int getTargetProgress() {
        return targetProgress;
    }

    public void setTargetProgress(int targetProgress) {
        this.targetProgress = targetProgress;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
