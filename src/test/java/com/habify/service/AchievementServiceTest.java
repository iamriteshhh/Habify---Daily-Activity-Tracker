package com.habify.service;

import com.habify.dto.AchievementDTO;
import com.habify.dto.StreakDTO;
import com.habify.entity.Goal;
import com.habify.entity.GoalStatus;
import com.habify.entity.Habit;
import com.habify.entity.HabitStatus;
import com.habify.repository.GoalRepository;
import com.habify.repository.HabitLogRepository;
import com.habify.repository.HabitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitLogRepository habitLogRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private StreakService streakService;

    @Mock
    private ScoringService scoringService;

    private AchievementService achievementService;

    @BeforeEach
    void setUp() {
        achievementService = new AchievementService(
                habitRepository,
                habitLogRepository,
                goalRepository,
                streakService,
                scoringService
        );
    }

    @Test
    @DisplayName("Evaluate achievements for user with active streaks and completed goals")
    void testAchievementsEvaluation() {
        Long userId = 5L;

        Habit habit1 = new Habit();
        habit1.setId(1L);
        habit1.setIsActive(true);

        when(habitRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(habit1));
        when(habitRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(List.of(habit1));
        when(habitLogRepository.countByUserIdAndStatus(userId, HabitStatus.COMPLETED)).thenReturn(10L);
        when(streakService.calculateStreak(habit1)).thenReturn(new StreakDTO(1L, "habit1", 7, 10, 10));
        when(scoringService.calculateDisciplineScore(eq(userId), any())).thenReturn(92);

        Goal goal = new Goal();
        goal.setStatus(GoalStatus.COMPLETED);
        when(goalRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(goal));

        List<AchievementDTO> achievements = achievementService.getUserAchievements(userId);

        assertEquals(6, achievements.size());

        // First step should be unlocked (10 >= 1)
        AchievementDTO firstStep = achievements.stream().filter(a -> a.getId().equals("first_step")).findFirst().orElseThrow();
        assertTrue(firstStep.isUnlocked());
        assertEquals(100, firstStep.getProgressPercentage());

        // Week warrior should be unlocked (max streak 10 >= 7)
        AchievementDTO weekWarrior = achievements.stream().filter(a -> a.getId().equals("week_warrior")).findFirst().orElseThrow();
        assertTrue(weekWarrior.isUnlocked());

        // Routine builder needs 5 active habits, user has 1 -> not unlocked, 20%
        AchievementDTO routineBuilder = achievements.stream().filter(a -> a.getId().equals("routine_builder")).findFirst().orElseThrow();
        assertFalse(routineBuilder.isUnlocked());
        assertEquals(20, routineBuilder.getProgressPercentage());

        // Discipline elite should be unlocked (score 92 >= 90)
        AchievementDTO disciplineElite = achievements.stream().filter(a -> a.getId().equals("discipline_elite")).findFirst().orElseThrow();
        assertTrue(disciplineElite.isUnlocked());

        // Goal getter should be unlocked (1 completed goal)
        AchievementDTO goalGetter = achievements.stream().filter(a -> a.getId().equals("goal_getter")).findFirst().orElseThrow();
        assertTrue(goalGetter.isUnlocked());
    }
}
