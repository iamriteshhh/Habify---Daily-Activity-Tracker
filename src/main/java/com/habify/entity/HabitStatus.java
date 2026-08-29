package com.habify.entity;

/**
 * HabitStatus
 * 
 * Completion status for a habit on a specific date:
 * - COMPLETED: Habit was fully completed (100%).
 * - PARTIAL: Habit was partially completed (1% - 99%).
 * - MISSED: Habit was not completed (0%).
 */
public enum HabitStatus {
    COMPLETED,
    PARTIAL,
    MISSED
}
