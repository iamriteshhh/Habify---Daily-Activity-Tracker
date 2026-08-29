package com.habify.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * HabitLog Entity
 * 
 * Represents the record of activity for a specific Habit on a given date.
 * Enforces one log per habit per date via database unique constraint.
 */
@Entity
@Table(name = "habit_logs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"habit_id", "log_date"}, name = "uq_habit_date")
})
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HabitStatus status = HabitStatus.COMPLETED;

    @Column(name = "completion_percentage", nullable = false)
    private Integer completionPercentage = 100;

    @Column(name = "completion_time")
    private LocalTime completionTime;

    @Column(length = 300)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public HabitLog() {
    }

    public HabitLog(Habit habit, LocalDate logDate, HabitStatus status, Integer completionPercentage, String note) {
        this.habit = habit;
        this.logDate = logDate;
        this.status = status;
        this.completionPercentage = completionPercentage;
        this.note = note;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.completionTime == null && this.status != HabitStatus.MISSED) {
            this.completionTime = LocalTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Habit getHabit() {
        return habit;
    }

    public void setHabit(Habit habit) {
        this.habit = habit;
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

    public LocalTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalTime completionTime) {
        this.completionTime = completionTime;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
