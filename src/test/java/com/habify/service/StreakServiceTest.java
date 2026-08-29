package com.habify.service;

import com.habify.dto.StreakDTO;
import com.habify.entity.FrequencyType;
import com.habify.entity.Habit;
import com.habify.entity.HabitLog;
import com.habify.entity.HabitStatus;
import com.habify.repository.HabitLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private HabitLogRepository habitLogRepository;

    private StreakService streakService;

    @BeforeEach
    void setUp() {
        streakService = new StreakService(habitLogRepository);
    }

    @Test
    @DisplayName("Daily Habit: 3 consecutive completed days calculates current streak of 3")
    void testDailyHabitContinuousStreak() {
        LocalDate today = LocalDate.now();

        Habit habit = new Habit();
        habit.setId(1L);
        habit.setTitle("Morning Workout");
        habit.setFrequencyType(FrequencyType.DAILY);
        habit.setIsActive(true);
        habit.setCreatedAt(LocalDateTime.now().minusDays(10));

        List<HabitLog> logs = new ArrayList<>();
        logs.add(new HabitLog(habit, today, HabitStatus.COMPLETED, 100, "Done"));
        logs.add(new HabitLog(habit, today.minusDays(1), HabitStatus.COMPLETED, 100, "Done"));
        logs.add(new HabitLog(habit, today.minusDays(2), HabitStatus.COMPLETED, 100, "Done"));

        when(habitLogRepository.findPastLogsByHabitId(eq(1L), any(LocalDate.class))).thenReturn(logs);

        StreakDTO result = streakService.calculateStreak(habit);

        assertEquals(3, result.getCurrentStreak());
        assertEquals(3, result.getLongestStreak());
        assertEquals(3, result.getTotalCompletedDays());
    }

    @Test
    @DisplayName("Specific Days Habit: Non-scheduled rest days do NOT break streak")
    void testSpecificDaysHabitRespectsRestDays() {
        LocalDate today = LocalDate.now();

        Habit habit = new Habit();
        habit.setId(2L);
        habit.setTitle("DSA Practice");
        habit.setFrequencyType(FrequencyType.SPECIFIC_DAYS);
        // Habit scheduled today and 2 days ago
        String todayDay = today.getDayOfWeek().name();
        String pastDay = today.minusDays(2).getDayOfWeek().name();
        habit.setScheduledDays(todayDay + "," + pastDay);
        habit.setIsActive(true);
        habit.setCreatedAt(LocalDateTime.now().minusDays(10));

        List<HabitLog> logs = new ArrayList<>();
        logs.add(new HabitLog(habit, today, HabitStatus.COMPLETED, 100, "Today done"));
        logs.add(new HabitLog(habit, today.minusDays(2), HabitStatus.COMPLETED, 100, "Past scheduled day done"));

        when(habitLogRepository.findPastLogsByHabitId(eq(2L), any(LocalDate.class))).thenReturn(logs);

        StreakDTO result = streakService.calculateStreak(habit);

        // Even though yesterday was not completed, it was NOT scheduled, so streak is 2
        assertEquals(2, result.getCurrentStreak());
    }

    @Test
    @DisplayName("Missed scheduled day resets current streak to 0")
    void testMissedScheduledDayResetsStreak() {
        LocalDate today = LocalDate.now();

        Habit habit = new Habit();
        habit.setId(3L);
        habit.setTitle("Read Book");
        habit.setFrequencyType(FrequencyType.DAILY);
        habit.setIsActive(true);
        habit.setCreatedAt(LocalDateTime.now().minusDays(10));

        List<HabitLog> logs = new ArrayList<>();
        // Today is logged as MISSED
        logs.add(new HabitLog(habit, today, HabitStatus.MISSED, 0, "Too busy"));
        logs.add(new HabitLog(habit, today.minusDays(1), HabitStatus.COMPLETED, 100, "Done"));

        when(habitLogRepository.findPastLogsByHabitId(eq(3L), any(LocalDate.class))).thenReturn(logs);

        StreakDTO result = streakService.calculateStreak(habit);

        assertEquals(0, result.getCurrentStreak());
        assertEquals(1, result.getLongestStreak());
    }
}
