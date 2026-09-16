package com.habify.service;

import com.habify.dto.HabitRequest;
import com.habify.dto.HabitResponse;
import com.habify.dto.StreakDTO;
import com.habify.entity.*;
import com.habify.exception.BadRequestException;
import com.habify.exception.ResourceNotFoundException;
import com.habify.repository.HabitLogRepository;
import com.habify.repository.HabitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HabitService
 * 
 * Core habit management engine.
 * Handles habit lifecycle (create, read, update, deactivate, delete)
 * and frequency validation.
 */
@Service
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final UserService userService;
    private final StreakService streakService;

    public HabitService(HabitRepository habitRepository,
                        HabitLogRepository habitLogRepository,
                        UserService userService,
                        StreakService streakService) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.userService = userService;
        this.streakService = streakService;
    }

    /**
     * Creates a new habit for a user with frequency validation.
     */
    @Transactional
    public HabitResponse createHabit(Long userId, HabitRequest request) {
        User user = userService.getUserById(userId);
        validateHabitRequest(request);

        Habit habit = new Habit();
        habit.setUser(user);
        mapRequestToEntity(request, habit);

        Habit savedHabit = habitRepository.save(habit);
        return mapEntityToResponse(savedHabit, LocalDate.now());
    }

    /**
     * Retrieves all habits defined by the user with streak and status information.
     */
    public List<HabitResponse> getHabitsByUser(Long userId) {
        LocalDate today = LocalDate.now();
        List<Habit> habits = habitRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return habits.stream()
                .map(h -> mapEntityToResponse(h, today))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single habit by ID, verifying ownership.
     */
    public HabitResponse getHabitResponseById(Long habitId, Long userId) {
        Habit habit = getHabitEntityByIdAndUser(habitId, userId);
        return mapEntityToResponse(habit, LocalDate.now());
    }

    public Habit getHabitEntityByIdAndUser(Long habitId, Long userId) {
        return habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with ID: " + habitId));
    }

    /**
     * Updates an existing habit.
     */
    @Transactional
    public HabitResponse updateHabit(Long habitId, Long userId, HabitRequest request) {
        Habit habit = getHabitEntityByIdAndUser(habitId, userId);
        validateHabitRequest(request);

        mapRequestToEntity(request, habit);
        Habit updatedHabit = habitRepository.save(habit);

        return mapEntityToResponse(updatedHabit, LocalDate.now());
    }

    /**
     * Toggles habit active / inactive status.
     */
    @Transactional
    public HabitResponse toggleHabitActive(Long habitId, Long userId) {
        Habit habit = getHabitEntityByIdAndUser(habitId, userId);
        habit.setIsActive(!Boolean.TRUE.equals(habit.getIsActive()));
        Habit updated = habitRepository.save(habit);
        return mapEntityToResponse(updated, LocalDate.now());
    }

    /**
     * Deletes a habit and all associated logs.
     */
    @Transactional
    public void deleteHabit(Long habitId, Long userId) {
        Habit habit = getHabitEntityByIdAndUser(habitId, userId);
        habitLogRepository.deleteByHabitId(habit.getId());
        habitRepository.delete(habit);
    }

    /**
     * Validates frequency and scheduled days configuration.
     */
    private void validateHabitRequest(HabitRequest request) {
        if (request.getFrequencyType() == FrequencyType.SPECIFIC_DAYS) {
            if (request.getScheduledDays() == null || request.getScheduledDays().trim().isEmpty()) {
                throw new BadRequestException("Scheduled days must be specified when frequency type is SPECIFIC_DAYS.");
            }
            // Verify each day is a valid DayOfWeek
            String[] days = request.getScheduledDays().split(",");
            for (String day : days) {
                try {
                    DayOfWeek.valueOf(day.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Invalid day name: '" + day.trim() + "'. Allowed values: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY.");
                }
            }
        }
    }

    private void mapRequestToEntity(HabitRequest request, Habit habit) {
        habit.setTitle(request.getTitle().trim());
        habit.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        habit.setCategory(request.getCategory() != null ? request.getCategory().trim() : "Other");
        habit.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        habit.setFrequencyType(request.getFrequencyType() != null ? request.getFrequencyType() : FrequencyType.DAILY);
        
        if (habit.getFrequencyType() == FrequencyType.SPECIFIC_DAYS) {
            String sanitizedDays = Arrays.stream(request.getScheduledDays().split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.joining(","));
            habit.setScheduledDays(sanitizedDays);
        } else {
            habit.setScheduledDays(null);
        }

        habit.setTargetValue(request.getTargetValue() != null && request.getTargetValue() > 0 ? request.getTargetValue() : 1);
        habit.setUnit(request.getUnit() != null ? request.getUnit().trim() : "times");
        habit.setPreferredTime(request.getPreferredTime() != null ? request.getPreferredTime() : TimeOfDay.ANYTIME);
    }

    public HabitResponse mapEntityToResponse(Habit habit, LocalDate today) {
        HabitResponse response = new HabitResponse();
        response.setId(habit.getId());
        response.setTitle(habit.getTitle());
        response.setDescription(habit.getDescription());
        response.setCategory(habit.getCategory());
        response.setPriority(habit.getPriority());
        response.setFrequencyType(habit.getFrequencyType());
        response.setScheduledDays(habit.getScheduledDays());
        response.setScheduledDaysSet(habit.getScheduledDaysSet());
        response.setTargetValue(habit.getTargetValue());
        response.setUnit(habit.getUnit());
        response.setPreferredTime(habit.getPreferredTime());
        response.setIsActive(habit.getIsActive());
        response.setCreatedAt(habit.getCreatedAt());
        response.setScheduledToday(habit.isScheduledFor(today));

        // Streaks
        StreakDTO streakDTO = streakService.calculateStreak(habit);
        response.setCurrentStreak(streakDTO.getCurrentStreak());
        response.setLongestStreak(streakDTO.getLongestStreak());

        // Today's log
        Optional<HabitLog> todayLog = habitLogRepository.findByHabitIdAndLogDate(habit.getId(), today);
        if (todayLog.isPresent()) {
            response.setTodayStatus(todayLog.get().getStatus());
            response.setTodayPercentage(todayLog.get().getCompletionPercentage());
        }

        return response;
    }
}
