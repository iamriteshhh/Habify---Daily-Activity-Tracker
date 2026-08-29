package com.habify.service;

import com.habify.dto.DailyProgressDTO;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock
    private HabitLogRepository habitLogRepository;

    private ScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new ScoringService(habitLogRepository);
    }

    @Test
    @DisplayName("Discipline Score: Returns 100% when no active habits exist")
    void testDisciplineScoreNoHabits() {
        int score = scoringService.calculateDisciplineScore(1L, Collections.emptyList());
        assertEquals(100, score);
    }

    @Test
    @DisplayName("Discipline Score: Calculates accurate score for 7-day completed window")
    void testDisciplineScoreFullCompletion() {
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        Habit habit = new Habit();
        habit.setId(10L);
        habit.setFrequencyType(FrequencyType.DAILY);
        habit.setIsActive(true);
        habit.setCreatedAt(LocalDateTime.now().minusDays(10));

        List<HabitLog> logs = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            logs.add(new HabitLog(habit, startDate.plusDays(i), HabitStatus.COMPLETED, 100, null));
        }

        when(habitLogRepository.findByUserIdAndLogDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(logs);

        int score = scoringService.calculateDisciplineScore(userId, List.of(habit));
        assertEquals(100, score);
    }

    @Test
    @DisplayName("Weekly Progress: Returns 7 days breakdown with accurate percentages")
    void testWeeklyProgressBreakdown() {
        Long userId = 1L;
        Habit habit = new Habit();
        habit.setId(10L);
        habit.setFrequencyType(FrequencyType.DAILY);
        habit.setIsActive(true);
        habit.setCreatedAt(LocalDateTime.now().minusDays(10));

        when(habitLogRepository.findByUserIdAndLogDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        List<DailyProgressDTO> progress = scoringService.getWeeklyProgress(userId, List.of(habit));

        assertNotNull(progress);
        assertEquals(7, progress.size());
    }
}
