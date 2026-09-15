package com.habify.service;

import com.habify.dto.*;
import com.habify.entity.Habit;
import com.habify.entity.HabitLog;
import com.habify.entity.HabitStatus;
import com.habify.repository.HabitLogRepository;
import com.habify.repository.HabitRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AnalyticsService
 * 
 * Aggregates analytical feedback, discipline scores, category distributions,
 * streaks, best/weakest days, and rule-based insights for the Habify dashboard.
 */
@Service
public class AnalyticsService {

    private final HabitRepository habitRepository;
    private final HabitService habitService;
    private final HabitLogService habitLogService;
    private final HabitLogRepository habitLogRepository;
    private final ScoringService scoringService;
    private final StreakService streakService;

    public AnalyticsService(HabitRepository habitRepository,
                            HabitService habitService,
                            HabitLogService habitLogService,
                            HabitLogRepository habitLogRepository,
                            ScoringService scoringService,
                            StreakService streakService) {
        this.habitRepository = habitRepository;
        this.habitService = habitService;
        this.habitLogService = habitLogService;
        this.habitLogRepository = habitLogRepository;
        this.scoringService = scoringService;
        this.streakService = streakService;
    }

    /**
     * Compiles complete dashboard summary with KPIs, streaks, discipline score,
     * weekly progress, category breakdown, best/weakest days, and rule-based insights.
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

        // Compute Category Distribution
        List<CategoryProgressDTO> categoryDistribution = calculateCategoryDistribution(userId, activeHabits);

        // Calculate Best and Weakest Days & Weekly Average
        String bestDay = "N/A";
        String weakestDay = "N/A";
        int maxPct = -1;
        int minPct = 101;
        int sumWeeklyPct = 0;
        int daysWithScheduled = 0;

        for (DailyProgressDTO dp : weeklyProgress) {
            if (dp.getTotalScheduled() > 0) {
                daysWithScheduled++;
                sumWeeklyPct += dp.getCompletionPercentage();
                if (dp.getCompletionPercentage() > maxPct) {
                    maxPct = dp.getCompletionPercentage();
                    bestDay = dp.getDayOfWeek() + " (" + dp.getCompletionPercentage() + "%)";
                }
                if (dp.getCompletionPercentage() < minPct) {
                    minPct = dp.getCompletionPercentage();
                    weakestDay = dp.getDayOfWeek() + " (" + dp.getCompletionPercentage() + "%)";
                }
            }
        }

        int weeklyAveragePercentage = daysWithScheduled > 0 ? Math.round((float) sumWeeklyPct / daysWithScheduled) : 0;

        // Generate Rule-Based Insights
        List<InsightDTO> insights = generateRuleBasedInsights(weeklyProgress, categoryDistribution, bestCurrentStreak, disciplineScore);

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
        response.setCategoryDistribution(categoryDistribution);
        response.setInsights(insights);
        response.setBestDay(bestDay);
        response.setWeakestDay(weakestDay);
        response.setWeeklyAveragePercentage(weeklyAveragePercentage);

        return response;
    }

    /**
     * Calculates category progress distribution over the last 7 days.
     */
    private List<CategoryProgressDTO> calculateCategoryDistribution(Long userId, List<Habit> activeHabits) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        List<HabitLog> logs = habitLogRepository.findByUserIdAndLogDateBetween(userId, startDate, today);

        Map<Long, List<HabitLog>> logsByHabitId = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getHabit().getId()));

        Map<String, int[]> categoryStats = new LinkedHashMap<>(); // [scheduled, completed, partial, missed, earnedPct]

        for (Habit habit : activeHabits) {
            String category = (habit.getCategory() != null && !habit.getCategory().trim().isEmpty())
                    ? habit.getCategory().trim()
                    : "General";

            categoryStats.putIfAbsent(category, new int[5]);
            int[] stats = categoryStats.get(category);

            List<HabitLog> habitLogs = logsByHabitId.getOrDefault(habit.getId(), Collections.emptyList());
            Map<LocalDate, HabitLog> logDateMap = habitLogs.stream()
                    .collect(Collectors.toMap(HabitLog::getLogDate, l -> l, (a, b) -> a));

            for (int i = 0; i < 7; i++) {
                LocalDate d = startDate.plusDays(i);
                if (habit.isScheduledFor(d)) {
                    stats[0]++; // scheduled
                    HabitLog log = logDateMap.get(d);
                    if (log != null) {
                        if (log.getStatus() == HabitStatus.COMPLETED) {
                            stats[1]++;
                            stats[4] += 100;
                        } else if (log.getStatus() == HabitStatus.PARTIAL) {
                            stats[2]++;
                            stats[4] += (log.getCompletionPercentage() != null ? log.getCompletionPercentage() : 50);
                        } else if (log.getStatus() == HabitStatus.MISSED) {
                            stats[3]++;
                        }
                    }
                }
            }
        }

        List<CategoryProgressDTO> dtos = new ArrayList<>();
        categoryStats.forEach((cat, s) -> {
            int scheduled = s[0];
            int completed = s[1];
            int partial = s[2];
            int missed = s[3];
            int earnedPct = s[4];
            int pct = scheduled > 0 ? Math.min(100, Math.round((float) earnedPct / scheduled)) : 0;
            dtos.add(new CategoryProgressDTO(cat, scheduled, completed, partial, missed, pct));
        });

        return dtos;
    }

    /**
     * Generates 3 to 5 rule-based behavioural insights.
     */
    private List<InsightDTO> generateRuleBasedInsights(List<DailyProgressDTO> weeklyProgress,
                                                       List<CategoryProgressDTO> categories,
                                                       int bestStreak,
                                                       int disciplineScore) {
        List<InsightDTO> list = new ArrayList<>();

        // 1. Weekend vs Weekday rule
        int weekdaySum = 0, weekdayCount = 0;
        int weekendSum = 0, weekendCount = 0;

        for (DailyProgressDTO dp : weeklyProgress) {
            DayOfWeek dow = dp.getDate().getDayOfWeek();
            if (dp.getTotalScheduled() > 0) {
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                    weekendSum += dp.getCompletionPercentage();
                    weekendCount++;
                } else {
                    weekdaySum += dp.getCompletionPercentage();
                    weekdayCount++;
                }
            }
        }

        if (weekdayCount > 0 && weekendCount > 0) {
            float weekdayAvg = (float) weekdaySum / weekdayCount;
            float weekendAvg = (float) weekendSum / weekendCount;
            if (weekdayAvg - weekendAvg >= 15) {
                list.add(new InsightDTO(
                        "WARNING",
                        "Weekend Consistency Drop",
                        "Your consistency drops by " + Math.round(weekdayAvg - weekendAvg) + "% on weekends. Consider planning lighter weekend routines or setting planned rest days."
                ));
            } else if (weekendAvg >= weekdayAvg && weekendAvg >= 70) {
                list.add(new InsightDTO(
                        "SUCCESS",
                        "Strong Weekend Habit Discipline",
                        "You maintain strong consistency through Saturday and Sunday, keeping your habits resilient to routine changes."
                ));
            }
        }

        // 2. Streak milestone rule
        if (bestStreak >= 7) {
            list.add(new InsightDTO(
                    "SUCCESS",
                    "7+ Day Streak Momentum",
                    "Your best active streak is " + bestStreak + " days! Habit automaticity builds rapidly after the first week of sustained consistency."
            ));
        } else if (bestStreak > 0) {
            list.add(new InsightDTO(
                    "INFO",
                    "Momentum in Progress",
                    "Active streak is " + bestStreak + " days. Showing up daily turns conscious effort into an effortless ritual."
            ));
        } else {
            list.add(new InsightDTO(
                    "INFO",
                    "Ready for a Fresh Streak",
                    "Complete all your scheduled activities today to kickstart a brand new consistency streak."
            ));
        }

        // 3. Top Category Focus rule
        if (!categories.isEmpty()) {
            CategoryProgressDTO topCat = categories.stream()
                    .max(Comparator.comparingInt(CategoryProgressDTO::getCompletionPercentage))
                    .orElse(null);

            if (topCat != null && topCat.getCompletionPercentage() >= 60) {
                list.add(new InsightDTO(
                        "SUCCESS",
                        "Leading Category: " + topCat.getCategory(),
                        "Your strongest category this week is " + topCat.getCategory() + " with " + topCat.getCompletionPercentage() + "% completion."
                ));
            }
        }

        // 4. Discipline Rating rule
        if (disciplineScore >= 80) {
            list.add(new InsightDTO(
                    "SUCCESS",
                    "Elite Discipline Score",
                    "Your 7-day rolling discipline rating is " + disciplineScore + "/100. You are performing in the top consistency tier."
            ));
        } else if (disciplineScore < 50) {
            list.add(new InsightDTO(
                    "WARNING",
                    "Opportunity to Reset",
                    "Your discipline score is currently " + disciplineScore + "/100. Try focusing on just 1 or 2 core habits tomorrow to build back momentum."
            ));
        }

        return list;
    }

    /**
     * Retrieves streak metrics for a specific habit.
     */
    public StreakDTO getHabitStreak(Long userId, Long habitId) {
        Habit habit = habitService.getHabitEntityByIdAndUser(habitId, userId);
        return streakService.calculateStreak(habit);
    }
}
