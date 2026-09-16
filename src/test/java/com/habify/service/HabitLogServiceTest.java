package com.habify.service;

import com.habify.dto.HabitLogRequest;
import com.habify.dto.HabitLogResponse;
import com.habify.entity.Goal;
import com.habify.entity.GoalStatus;
import com.habify.entity.Habit;
import com.habify.entity.HabitLog;
import com.habify.entity.HabitStatus;
import com.habify.exception.BadRequestException;
import com.habify.repository.GoalRepository;
import com.habify.repository.HabitLogRepository;
import com.habify.repository.HabitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitLogServiceTest {

    @Mock
    private HabitLogRepository habitLogRepository;

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private UserService userService;

    @Mock
    private GoalRepository goalRepository;

    private StreakService streakService;
    private HabitService habitService;
    private HabitLogService habitLogService;

    @BeforeEach
    void setUp() {
        streakService = new StreakService(habitLogRepository);
        habitService = new HabitService(habitRepository, habitLogRepository, userService, streakService);
        habitLogService = new HabitLogService(habitLogRepository, habitRepository, habitService, streakService, goalRepository);
    }

    @Test
    @DisplayName("Reject logging for future date with BadRequestException")
    void testRejectFutureDateLog() {
        Long userId = 1L;
        Long habitId = 5L;
        LocalDate futureDate = LocalDate.now().plusDays(2);

        Habit habit = new Habit();
        habit.setId(habitId);
        habit.setIsActive(true);

        when(habitRepository.findByIdAndUserId(eq(habitId), eq(userId))).thenReturn(Optional.of(habit));

        HabitLogRequest request = new HabitLogRequest(futureDate, HabitStatus.COMPLETED, 100, "Future test");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                habitLogService.logHabit(userId, habitId, request)
        );

        assertTrue(ex.getMessage().contains("future dates"));
    }

    @Test
    @DisplayName("Reject logging for inactive habit with BadRequestException")
    void testRejectInactiveHabitLog() {
        Long userId = 1L;
        Long habitId = 6L;

        Habit habit = new Habit();
        habit.setId(habitId);
        habit.setIsActive(false); // Inactive

        when(habitRepository.findByIdAndUserId(eq(habitId), eq(userId))).thenReturn(Optional.of(habit));

        HabitLogRequest request = new HabitLogRequest(LocalDate.now(), HabitStatus.COMPLETED, 100, "Inactive test");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                habitLogService.logHabit(userId, habitId, request)
        );

        assertTrue(ex.getMessage().contains("inactive habit"));
    }

    @Test
    @DisplayName("Log habit successfully saves record")
    void testSuccessfulHabitLog() {
        Long userId = 1L;
        Long habitId = 7L;
        LocalDate today = LocalDate.now();

        Habit habit = new Habit();
        habit.setId(habitId);
        habit.setTitle("Meditation");
        habit.setIsActive(true);

        when(habitRepository.findByIdAndUserId(eq(habitId), eq(userId))).thenReturn(Optional.of(habit));
        when(habitLogRepository.findByHabitIdAndLogDate(eq(habitId), eq(today))).thenReturn(Optional.empty());

        HabitLog savedLog = new HabitLog(habit, today, HabitStatus.COMPLETED, 100, "Calm mind");
        savedLog.setId(101L);

        when(habitLogRepository.save(any(HabitLog.class))).thenReturn(savedLog);

        HabitLogRequest request = new HabitLogRequest(today, HabitStatus.COMPLETED, 100, "Calm mind");
        HabitLogResponse response = habitLogService.logHabit(userId, habitId, request);

        assertNotNull(response);
        assertEquals(HabitStatus.COMPLETED, response.getStatus());
        assertEquals(100, response.getCompletionPercentage());
    }

    @Test
    @DisplayName("Log Habit: Completing a habit automatically advances active goal progress")
    void testSuccessfulHabitLogAdvancesActiveGoals() {
        Long userId = 1L;
        Long habitId = 8L;
        LocalDate today = LocalDate.now();

        Habit habit = new Habit();
        habit.setId(habitId);
        habit.setIsActive(true);

        when(habitRepository.findByIdAndUserId(eq(habitId), eq(userId))).thenReturn(Optional.of(habit));
        when(habitLogRepository.findByHabitIdAndLogDate(eq(habitId), eq(today))).thenReturn(Optional.empty());

        HabitLog savedLog = new HabitLog(habit, today, HabitStatus.COMPLETED, 100, "Done");
        when(habitLogRepository.save(any(HabitLog.class))).thenReturn(savedLog);

        Goal goal = new Goal();
        goal.setId(10L);
        goal.setTargetType("HABIT_LOGS");
        goal.setCurrentValue(4);
        goal.setTargetValue(5);
        goal.setStatus(GoalStatus.ACTIVE);

        when(goalRepository.findByUserIdAndStatus(eq(userId), eq(GoalStatus.ACTIVE))).thenReturn(List.of(goal));

        HabitLogRequest request = new HabitLogRequest(today, HabitStatus.COMPLETED, 100, null);
        habitLogService.logHabit(userId, habitId, request);

        assertEquals(5, goal.getCurrentValue());
        assertEquals(GoalStatus.COMPLETED, goal.getStatus());
        verify(goalRepository).save(goal);
    }
}
