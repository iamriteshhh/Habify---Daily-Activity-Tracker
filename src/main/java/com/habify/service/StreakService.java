package com.habify.service;

import com.habify.dto.StreakDTO;
import com.habify.entity.Habit;
import com.habify.entity.HabitLog;
import com.habify.entity.HabitStatus;
import com.habify.repository.HabitLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StreakService
 * 
 * Responsible for frequency-aware streak calculations for Habify habits.
 * 
 * Key Rules:
 * 1. Rest/Non-scheduled Days: If a habit is only scheduled on certain days (e.g., Mon/Wed/Fri),
 *    non-scheduled days (Tue/Thu/Sat/Sun) do NOT break the streak.
 * 2. Completion: A day counts towards a streak if logged as COMPLETED or completionPercentage >= 50.
 * 3. Today's Pending Status: If today is a scheduled day but not yet logged, the active streak up to
 *    yesterday remains intact.
 * 4. Missed Scheduled Day: A scheduled day that passed without completion breaks the active streak.
 * 5. Habit Inception: Does not look back prior to the habit's creation date.
 */
@Service
public class StreakService {

    private final HabitLogRepository habitLogRepository;

    public StreakService(HabitLogRepository habitLogRepository) {
        this.habitLogRepository = habitLogRepository;
    }

    /**
     * Calculates the current streak, longest streak, and total completed days for a given habit.
     */
    public StreakDTO calculateStreak(Habit habit) {
        LocalDate today = LocalDate.now();
        List<HabitLog> logs = habitLogRepository.findPastLogsByHabitId(habit.getId(), today);

        Map<LocalDate, HabitLog> logMap = logs.stream()
                .collect(Collectors.toMap(HabitLog::getLogDate, log -> log, (existing, replace) -> existing));

        int currentStreak = calculateCurrentStreak(habit, today, logMap);
        int longestStreak = calculateLongestStreak(habit, logs);
        int totalCompletedDays = (int) logs.stream()
                .filter(l -> l.getStatus() == HabitStatus.COMPLETED || l.getCompletionPercentage() >= 50)
                .count();

        return new StreakDTO(habit.getId(), habit.getTitle(), currentStreak, longestStreak, totalCompletedDays);
    }

    /**
     * Computes the ongoing continuous streak backwards from today or yesterday.
     */
    public int calculateCurrentStreak(Habit habit, LocalDate today, Map<LocalDate, HabitLog> logMap) {
        int streak = 0;
        LocalDate checkDate = today;

        // If today is scheduled, check if it's logged today
        if (habit.isScheduledFor(today)) {
            HabitLog todayLog = logMap.get(today);
            if (todayLog != null) {
                if (isSuccessful(todayLog)) {
                    streak++;
                    checkDate = today.minusDays(1);
                } else {
                    // Logged as MISSED today -> streak broken
                    return 0;
                }
            } else {
                // Today is not yet logged -> check streak up to yesterday
                checkDate = today.minusDays(1);
            }
        } else {
            // Today is not scheduled -> check backwards from yesterday
            checkDate = today.minusDays(1);
        }

        // Walk backwards day-by-day up to the habit creation date
        LocalDate minDate = habit.getCreatedAt() != null 
                ? habit.getCreatedAt().toLocalDate() 
                : today.minusDays(365);

        while (!checkDate.isBefore(minDate)) {
            if (habit.isScheduledFor(checkDate)) {
                HabitLog log = logMap.get(checkDate);
                if (log != null && isSuccessful(log)) {
                    streak++;
                } else {
                    // Scheduled day was missed or incomplete -> streak stops
                    break;
                }
            }
            // If checkDate was not scheduled, we simply skip it without breaking the streak
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    /**
     * Computes the maximum historic streak recorded for this habit.
     */
    public int calculateLongestStreak(Habit habit, List<HabitLog> logs) {
        if (logs.isEmpty()) {
            return 0;
        }

        // Sort logs in ascending order of date
        List<HabitLog> sortedLogs = new ArrayList<>(logs);
        sortedLogs.sort(Comparator.comparing(HabitLog::getLogDate));

        Map<LocalDate, HabitLog> logMap = sortedLogs.stream()
                .collect(Collectors.toMap(HabitLog::getLogDate, l -> l, (a, b) -> a));

        LocalDate startDate = sortedLogs.get(0).getLogDate();
        LocalDate endDate = sortedLogs.get(sortedLogs.size() - 1).getLogDate();

        int maxStreak = 0;
        int currentCount = 0;
        LocalDate curr = startDate;

        while (!curr.isAfter(endDate)) {
            if (habit.isScheduledFor(curr)) {
                HabitLog log = logMap.get(curr);
                if (log != null && isSuccessful(log)) {
                    currentCount++;
                    if (currentCount > maxStreak) {
                        maxStreak = currentCount;
                    }
                } else {
                    currentCount = 0;
                }
            }
            curr = curr.plusDays(1);
        }

        return maxStreak;
    }

    private boolean isSuccessful(HabitLog log) {
        if (log == null) return false;
        return log.getStatus() == HabitStatus.COMPLETED || 
               (log.getCompletionPercentage() != null && log.getCompletionPercentage() >= 50);
    }
}
