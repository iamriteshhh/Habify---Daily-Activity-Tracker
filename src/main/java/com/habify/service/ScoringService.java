package com.habify.service;

import com.habify.dto.DailyProgressDTO;
import com.habify.entity.Habit;
import com.habify.entity.HabitLog;
import com.habify.entity.HabitStatus;
import com.habify.repository.HabitLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ScoringService
 * 
 * Calculates personal discipline and consistency metrics for Habify users.
 * 
 * Discipline Score Formula:
 * The score measures performance over a rolling 7-day window.
 * Score = (Sum of Completion Percentages for all scheduled habit days in window) / (Total scheduled habit instances)
 * 
 * Edge cases:
 * - If no habits are scheduled in the window, returns 100 as the baseline.
 * - Non-scheduled days for a habit do not penalize the score.
 * - Days prior to a habit's creation are not penalized.
 */
@Service
public class ScoringService {

    private final HabitLogRepository habitLogRepository;

    public ScoringService(HabitLogRepository habitLogRepository) {
        this.habitLogRepository = habitLogRepository;
    }

    /**
     * Calculates the 7-day rolling Discipline Score (0 - 100).
     */
    public int calculateDisciplineScore(Long userId, List<Habit> activeHabits) {
        if (activeHabits.isEmpty()) {
            return 100;
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6); // 7-day window

        List<HabitLog> logs = habitLogRepository.findByUserIdAndLogDateBetween(userId, startDate, today);
        Map<String, HabitLog> logMap = logs.stream()
                .collect(Collectors.toMap(
                        l -> l.getHabit().getId() + "_" + l.getLogDate().toString(),
                        l -> l,
                        (existing, replace) -> existing
                ));

        int totalScheduledInstances = 0;
        int totalPercentageEarned = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            for (Habit habit : activeHabits) {
                LocalDate habitCreated = habit.getCreatedAt() != null ? habit.getCreatedAt().toLocalDate() : today;
                // Only evaluate days on or after the habit was created
                if (date.isBefore(habitCreated)) {
                    continue;
                }

                if (habit.isScheduledFor(date)) {
                    totalScheduledInstances++;
                    String key = habit.getId() + "_" + date.toString();
                    HabitLog log = logMap.get(key);

                    if (log != null) {
                        int pct = log.getCompletionPercentage() != null ? log.getCompletionPercentage() : 0;
                        if (log.getStatus() == HabitStatus.COMPLETED) {
                            pct = 100;
                        } else if (log.getStatus() == HabitStatus.MISSED) {
                            pct = 0;
                        }
                        totalPercentageEarned += pct;
                    } else {
                        // Not logged: if date has already passed before today, it counts as 0%
                        // If it's today and unlogged, 0% earned so far for today's scheduled instance
                        totalPercentageEarned += 0;
                    }
                }
            }
        }

        if (totalScheduledInstances == 0) {
            return 100;
        }

        double score = (double) totalPercentageEarned / totalScheduledInstances;
        return (int) Math.round(score);
    }

    /**
     * Builds weekly progress breakdown for charts and dashboard visualization.
     */
    public List<DailyProgressDTO> getWeeklyProgress(Long userId, List<Habit> activeHabits) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        List<HabitLog> logs = habitLogRepository.findByUserIdAndLogDateBetween(userId, startDate, today);
        Map<LocalDate, List<HabitLog>> logsByDate = logs.stream()
                .collect(Collectors.groupingBy(HabitLog::getLogDate));

        List<DailyProgressDTO> weeklyList = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            String dayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            int scheduledCount = 0;
            for (Habit habit : activeHabits) {
                LocalDate habitCreated = habit.getCreatedAt() != null ? habit.getCreatedAt().toLocalDate() : today;
                if (!date.isBefore(habitCreated) && habit.isScheduledFor(date)) {
                    scheduledCount++;
                }
            }

            List<HabitLog> dateLogs = logsByDate.getOrDefault(date, Collections.emptyList());
            int completed = 0;
            int partial = 0;
            int missed = 0;
            int sumPct = 0;

            for (HabitLog log : dateLogs) {
                if (log.getStatus() == HabitStatus.COMPLETED) {
                    completed++;
                    sumPct += 100;
                } else if (log.getStatus() == HabitStatus.PARTIAL) {
                    partial++;
                    sumPct += (log.getCompletionPercentage() != null ? log.getCompletionPercentage() : 50);
                } else if (log.getStatus() == HabitStatus.MISSED) {
                    missed++;
                }
            }

            int completionPct = 0;
            if (scheduledCount > 0) {
                completionPct = (int) Math.round((double) sumPct / scheduledCount);
                if (completionPct > 100) completionPct = 100;
            }

            weeklyList.add(new DailyProgressDTO(date, dayName, scheduledCount, completed, partial, missed, completionPct));
        }

        return weeklyList;
    }
}
