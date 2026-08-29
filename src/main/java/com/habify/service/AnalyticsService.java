package com.habify.service;

import com.habify.dto.DailyProgressDTO;
import com.habify.dto.DashboardSummaryResponse;
import com.habify.dto.StreakDTO;
import com.habify.dto.TodayHabitDTO;
import com.habify.entity.Habit;
import com.habify.entity.HabitStatus;
import com.habify.repository.HabitRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * AnalyticsService
 * 
 * Aggregates analytical feedback, discipline scores, streaks, and progress
 * metrics for the Habify dashboard.
 */
@Service
public class AnalyticsService {

    private final HabitRepository habitRepository;
    private final HabitService habitService;
    private final HabitLogService habitLogService;
    private final ScoringService scoringService;
    private final StreakService streakService;

    public AnalyticsService(HabitRepository habitRepository,
                            HabitService habitService,
                            HabitLogService habitLogService,
                            ScoringService scoringService,
                            StreakService streakService) {
        this.habitRepository = habitRepository;
        this.habitService = habitService;
        this.habitLogService = habitLogService;
        this.scoringService = scoringService;
        this.streakService = streakService;
    }

    /**
     * Compiles complete dashboard summary with KPIs, streaks, discipline score,
     * weekly progress, and today's habit list.
     */
    public DashboardSummaryResponse getDashboardSummary(Long userId) {
        LocalDate today = LocalDate.now();
        List<Habit> activeHabits = habitRepository.findByUserIdAndIsActiveTrue(userId);
        long totalHabits = habitRepository.countByUserId(userId);

        List<TodayHabitDTO> todayHabits = habitLogService.getTodayHabits(userId);

        int scheduledTodayCount = 0;
        int completedTodayCount = 0;
        int partialTodayCount = 0;
        int missedTodayCount = 0;
        int totalEarnedPercentageToday = 0;
        int bestCurrentStreak = 0;

        for (TodayHabitDTO th : todayHabits) {
            if (th.isScheduledToday()) {
                scheduledTodayCount++;
                if (th.getStatus() == HabitStatus.COMPLETED) {
                    completedTodayCount++;
                    totalEarnedPercentageToday += 100;
                } else if (th.getStatus() == HabitStatus.PARTIAL) {
                    partialTodayCount++;
                    totalEarnedPercentageToday += (th.getCompletionPercentage() != null ? th.getCompletionPercentage() : 50);
                } else if (th.getStatus() == HabitStatus.MISSED) {
                    missedTodayCount++;
                }
            }
            if (th.getCurrentStreak() > bestCurrentStreak) {
                bestCurrentStreak = th.getCurrentStreak();
            }
        }

        int todayCompletionPercentage = 0;
        if (scheduledTodayCount > 0) {
            todayCompletionPercentage = (int) Math.round((double) totalEarnedPercentageToday / scheduledTodayCount);
            if (todayCompletionPercentage > 100) todayCompletionPercentage = 100;
        }

        int disciplineScore = scoringService.calculateDisciplineScore(userId, activeHabits);
        List<DailyProgressDTO> weeklyProgress = scoringService.getWeeklyProgress(userId, activeHabits);

        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setTodayDate(today);
        response.setTotalHabits(totalHabits);
        response.setScheduledTodayCount(scheduledTodayCount);
        response.setCompletedTodayCount(completedTodayCount);
        response.setPartialTodayCount(partialTodayCount);
        response.setMissedTodayCount(missedTodayCount);
        response.setTodayCompletionPercentage(todayCompletionPercentage);
        response.setDisciplineScore(disciplineScore);
        response.setBestCurrentStreak(bestCurrentStreak);
        response.setWeeklyProgress(weeklyProgress);
        response.setTodayHabits(todayHabits);

        return response;
    }

    /**
     * Retrieves streak metrics for a specific habit.
     */
    public StreakDTO getHabitStreak(Long userId, Long habitId) {
        Habit habit = habitService.getHabitEntityByIdAndUser(habitId, userId);
        return streakService.calculateStreak(habit);
    }
}
