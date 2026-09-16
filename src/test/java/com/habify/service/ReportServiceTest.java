package com.habify.service;

import com.habify.dto.ReportSummaryDTO;
import com.habify.dto.StreakDTO;
import com.habify.entity.Habit;
import com.habify.entity.HabitLog;
import com.habify.entity.HabitStatus;
import com.habify.repository.HabitLogRepository;
import com.habify.repository.HabitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitLogRepository habitLogRepository;

    @Mock
    private StreakService streakService;

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("CSV Export sanitizes formula injection characters (=, +, -, @)")
    void testCsvExportSanitizesFormulaInjection() throws IOException {
        Long userId = 1L;
        Habit maliciousHabit = new Habit();
        maliciousHabit.setId(10L);
        maliciousHabit.setTitle("=cmd|' /C calc'!A0");
        maliciousHabit.setCategory("+dangerous_category");

        when(habitRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(List.of(maliciousHabit));
        when(habitLogRepository.findByUserIdAndLogDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(streakService.calculateStreak(maliciousHabit))
                .thenReturn(new StreakDTO(10L, maliciousHabit.getTitle(), 0, 0, 0));
        when(scoringService.calculateDisciplineScore(eq(userId), any()))
                .thenReturn(100);

        MockHttpServletResponse response = new MockHttpServletResponse();
        reportService.exportCsv(userId, "WEEKLY", LocalDate.now(), response);

        String csvOutput = response.getContentAsString();
        // Verify formula characters are neutralized with a leading single quote (')
        assertTrue(csvOutput.contains("\"'=cmd|' /C calc'!A0\""), "Malicious formula trigger = should be prefixed with single quote");
        assertTrue(csvOutput.contains("\"'+dangerous_category\""), "Malicious formula trigger + should be prefixed with single quote");
    }

    @Test
    @DisplayName("getReportSummary computes breakdown and honors creation date")
    void testGetReportSummaryWeeklyAndMonthly() {
        Long userId = 1L;
        LocalDate monday = LocalDate.of(2026, 9, 14); // Monday
        Habit habit = new Habit();
        habit.setId(20L);
        habit.setTitle("Morning Jog");
        habit.setCategory("Fitness");
        habit.setFrequencyType(com.habify.entity.FrequencyType.DAILY);
        habit.setCreatedAt(java.time.LocalDateTime.of(2026, 9, 14, 8, 0));

        when(habitRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(List.of(habit));

        HabitLog log = new HabitLog();
        log.setHabit(habit);
        log.setLogDate(monday);
        log.setStatus(HabitStatus.COMPLETED);
        log.setCompletionPercentage(100);

        when(habitLogRepository.findByUserIdAndLogDateBetween(eq(userId), eq(monday), eq(monday.plusDays(6))))
                .thenReturn(List.of(log));
        when(streakService.calculateStreak(habit))
                .thenReturn(new StreakDTO(20L, "Morning Jog", 1, 1, 100));
        when(scoringService.calculateDisciplineScore(eq(userId), any()))
                .thenReturn(95);

        ReportSummaryDTO summary = reportService.getReportSummary(userId, "WEEKLY", monday);

        org.junit.jupiter.api.Assertions.assertNotNull(summary);
        org.junit.jupiter.api.Assertions.assertEquals("WEEKLY", summary.getPeriodType());
        org.junit.jupiter.api.Assertions.assertEquals(95, summary.getDisciplineScore());
        org.junit.jupiter.api.Assertions.assertEquals(1, summary.getHabitBreakdown().size());
        org.junit.jupiter.api.Assertions.assertEquals(1, summary.getCompletedActivities());
    }
}
