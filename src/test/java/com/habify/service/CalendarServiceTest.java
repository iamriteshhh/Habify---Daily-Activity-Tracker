package com.habify.service;

import com.habify.dto.CalendarDayDTO;
import com.habify.dto.CalendarMonthDTO;
import com.habify.dto.HabitLogResponse;
import com.habify.entity.FrequencyType;
import com.habify.entity.Habit;
import com.habify.entity.HabitLog;
import com.habify.entity.HabitStatus;
import com.habify.repository.HabitLogRepository;
import com.habify.repository.HabitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private HabitLogRepository habitLogRepository;

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitLogService habitLogService;

    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService(habitLogRepository, habitRepository, habitLogService);
    }

    @Test
    @DisplayName("Month View: Habit created mid-month is not scheduled on prior days")
    void testMonthViewExcludesDaysBeforeHabitCreation() {
        Long userId = 1L;
        int year = 2026;
        int month = 9; // September (30 days)

        Habit habit = new Habit();
        habit.setId(20L);
        habit.setFrequencyType(FrequencyType.DAILY);
        habit.setIsActive(true);
        // Created on September 20, 2026
        habit.setCreatedAt(LocalDateTime.of(2026, 9, 20, 10, 0));

        when(habitRepository.findByUserIdAndIsActiveTrue(eq(userId))).thenReturn(List.of(habit));
        when(habitLogRepository.findByUserIdAndLogDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        CalendarMonthDTO result = calendarService.getMonthView(userId, year, month);

        assertNotNull(result);
        assertEquals(30, result.getDays().size());

        // Days 1 through 19 should have 0 scheduled count
        for (int d = 1; d <= 19; d++) {
            CalendarDayDTO dayDTO = result.getDays().get(d - 1);
            assertEquals(0, dayDTO.getScheduledCount(), "Day " + d + " should not be scheduled before habit creation");
        }

        // Days 20 through 30 should have 1 scheduled count
        for (int d = 20; d <= 30; d++) {
            CalendarDayDTO dayDTO = result.getDays().get(d - 1);
            assertEquals(1, dayDTO.getScheduledCount(), "Day " + d + " should be scheduled on/after habit creation");
        }
    }

    @Test
    @DisplayName("Month View: Completed activity assigns proper intensity level and percentage")
    void testMonthViewCompletedIntensity() {
        Long userId = 1L;
        int year = 2026;
        int month = 9;

        Habit habit = new Habit();
        habit.setId(21L);
        habit.setFrequencyType(FrequencyType.DAILY);
        habit.setIsActive(true);
        habit.setCreatedAt(LocalDateTime.of(2026, 9, 1, 0, 0));

        LocalDate completedDate = LocalDate.of(2026, 9, 15);
        HabitLog log = new HabitLog(habit, completedDate, HabitStatus.COMPLETED, 100, "Done");

        when(habitRepository.findByUserIdAndIsActiveTrue(eq(userId))).thenReturn(List.of(habit));
        when(habitLogRepository.findByUserIdAndLogDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(log));

        HabitLogResponse res = new HabitLogResponse();
        res.setStatus(HabitStatus.COMPLETED);
        res.setCompletionPercentage(100);
        when(habitLogService.mapEntityToResponse(any(HabitLog.class))).thenReturn(res);

        CalendarMonthDTO result = calendarService.getMonthView(userId, year, month);

        CalendarDayDTO day15 = result.getDays().get(14); // 15th
        assertEquals(1, day15.getCompletedCount());
        assertEquals(100, day15.getCompletionPercentage());
        assertEquals(3, day15.getIntensityLevel()); // intensity 3 for >= 80%
    }
}
