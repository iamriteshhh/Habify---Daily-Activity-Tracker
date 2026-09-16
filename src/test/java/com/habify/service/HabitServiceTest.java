package com.habify.service;

import com.habify.dto.HabitRequest;
import com.habify.dto.HabitResponse;
import com.habify.dto.StreakDTO;
import com.habify.entity.FrequencyType;
import com.habify.entity.Habit;
import com.habify.entity.Priority;
import com.habify.entity.User;
import com.habify.exception.BadRequestException;
import com.habify.exception.ResourceNotFoundException;
import com.habify.repository.HabitLogRepository;
import com.habify.repository.HabitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitLogRepository habitLogRepository;

    @Mock
    private UserService userService;

    @Mock
    private StreakService streakService;

    private HabitService habitService;

    @BeforeEach
    void setUp() {
        habitService = new HabitService(habitRepository, habitLogRepository, userService, streakService);
    }

    @Test
    @DisplayName("Create Habit: Valid daily habit creates and maps response")
    void testCreateDailyHabit() {
        Long userId = 1L;
        User user = new User("John Doe", "john@bca.edu", "john_bca", "hash");
        user.setId(userId);

        HabitRequest req = new HabitRequest();
        req.setTitle("Read Java Concurrency");
        req.setCategory("Study");
        req.setPriority(Priority.HIGH);
        req.setFrequencyType(FrequencyType.DAILY);
        req.setTargetValue(1);
        req.setUnit("chapter");

        when(userService.getUserById(userId)).thenReturn(user);

        Habit saved = new Habit();
        saved.setId(50L);
        saved.setUser(user);
        saved.setTitle("Read Java Concurrency");
        saved.setCategory("Study");
        saved.setPriority(Priority.HIGH);
        saved.setFrequencyType(FrequencyType.DAILY);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setIsActive(true);

        when(habitRepository.save(any(Habit.class))).thenReturn(saved);
        when(streakService.calculateStreak(any(Habit.class))).thenReturn(new StreakDTO(50L, "Read Java Concurrency", 0, 0, 0));

        HabitResponse response = habitService.createHabit(userId, req);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("Read Java Concurrency", response.getTitle());
    }

    @Test
    @DisplayName("Create Habit: Invalid scheduled day name throws BadRequestException")
    void testCreateHabitInvalidDay() {
        Long userId = 1L;
        User user = new User("John Doe", "john@bca.edu", "john_bca", "hash");

        HabitRequest req = new HabitRequest();
        req.setTitle("Run");
        req.setFrequencyType(FrequencyType.SPECIFIC_DAYS);
        req.setScheduledDays("MONDAY,SOMEDAY");

        when(userService.getUserById(userId)).thenReturn(user);

        assertThrows(BadRequestException.class, () -> habitService.createHabit(userId, req));
    }

    @Test
    @DisplayName("Get Habit: Non-existent ID throws ResourceNotFoundException")
    void testGetHabitNotFound() {
        when(habitRepository.findByIdAndUserId(eq(999L), eq(1L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> habitService.getHabitResponseById(999L, 1L));
    }

    @Test
    @DisplayName("Delete Habit: Deleting habit purges associated logs first to preserve DB integrity")
    void testDeleteHabitCascadeLogs() {
        Long userId = 1L;
        Long habitId = 42L;
        Habit habit = new Habit();
        habit.setId(habitId);

        when(habitRepository.findByIdAndUserId(eq(habitId), eq(userId))).thenReturn(Optional.of(habit));

        habitService.deleteHabit(habitId, userId);

        verify(habitLogRepository).deleteByHabitId(habitId);
        verify(habitRepository).delete(habit);
    }
}
