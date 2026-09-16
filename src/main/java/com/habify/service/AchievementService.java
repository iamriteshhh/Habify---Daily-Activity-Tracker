package com.habify.service;

import com.habify.dto.AchievementDTO;
import com.habify.entity.GoalStatus;
import com.habify.entity.Habit;
import com.habify.entity.HabitStatus;
import com.habify.repository.GoalRepository;
import com.habify.repository.HabitLogRepository;
import com.habify.repository.HabitRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AchievementService
 * 
 * Computes rule-based achievement badges dynamically from user habits,
 * activity logs, active streaks, and goals.
 */
@Service
public class AchievementService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final GoalRepository goalRepository;
    private final StreakService streakService;
    private final ScoringService scoringService;

    public AchievementService(HabitRepository habitRepository,
                              HabitLogRepository habitLogRepository,
                              GoalRepository goalRepository,
                              StreakService streakService,
                              ScoringService scoringService) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.goalRepository = goalRepository;
        this.streakService = streakService;
        this.scoringService = scoringService;
    }

    /**
     * Evaluates all achievement criteria for the user.
     */
    public List<AchievementDTO> getUserAchievements(Long userId) {
        List<Habit> habits = habitRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Habit> activeHabits = habitRepository.findByUserIdAndIsActiveTrue(userId);

        // Fetch aggregate user metrics
        int totalCompletedLogs = (int) habitLogRepository.countByUserIdAndStatus(userId, HabitStatus.COMPLETED);

        int maxStreak = 0;
        for (Habit h : habits) {
            com.habify.dto.StreakDTO streak = streakService.calculateStreak(h);
            int cur = streak.getCurrentStreak();
            int longest = streak.getLongestStreak();
            if (cur > maxStreak) maxStreak = cur;
            if (longest > maxStreak) maxStreak = longest;
        }

        int disciplineScore = scoringService.calculateDisciplineScore(userId, activeHabits);

        long completedGoals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(g -> g.getStatus() == GoalStatus.COMPLETED)
                .count();

        List<AchievementDTO> achievements = new ArrayList<>();

        // 1. First Step
        achievements.add(buildAchievement(
                "first_step",
                "First Step",
                "Complete your first daily habit routine.",
                "🌱",
                totalCompletedLogs,
                1,
                "Starter"
        ));

        // 2. Week Warrior (7-day streak)
        achievements.add(buildAchievement(
                "week_warrior",
                "Week Warrior",
                "Maintain a 7-day consistency streak.",
                "🔥",
                maxStreak,
                7,
                "Consistency"
        ));

        // 3. Routine Builder (5 active habits)
        achievements.add(buildAchievement(
                "routine_builder",
                "Routine Builder",
                "Configure and maintain 5 active habits.",
                "⚡",
                activeHabits.size(),
                5,
                "Habits"
        ));

        // 4. Century Club (100 completed habits)
        achievements.add(buildAchievement(
                "century_club",
                "Century Club",
                "Complete 100 scheduled habit activities.",
                "💯",
                totalCompletedLogs,
                100,
                "Milestones"
        ));

        // 5. Discipline Elite (Discipline score >= 90)
        achievements.add(buildAchievement(
                "discipline_elite",
                "Discipline Elite",
                "Achieve a 90+ rolling discipline score.",
                "🏆",
                disciplineScore,
                90,
                "Mastery"
        ));

        // 6. Goal Getter (Complete at least 1 goal)
        achievements.add(buildAchievement(
                "goal_getter",
                "Goal Getter",
                "Reach target completion on a personal goal.",
                "🎯",
                (int) completedGoals,
                1,
                "Goals"
        ));

        return achievements;
    }

    private AchievementDTO buildAchievement(String id, String title, String description, String icon,
                                            int current, int target, String category) {
        boolean unlocked = current >= target;
        int pct = target > 0 ? Math.min(100, Math.round(((float) current / target) * 100)) : 0;
        return new AchievementDTO(id, title, description, icon, unlocked, Math.min(current, target), target, pct, category);
    }
}
