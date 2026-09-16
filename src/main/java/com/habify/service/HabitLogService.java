package com.habify.service;

import com.habify.dto.HabitLogRequest;
import com.habify.dto.HabitLogResponse;
import com.habify.dto.TodayHabitDTO;
import com.habify.entity.Goal;
import com.habify.entity.GoalStatus;
import com.habify.entity.Habit;
import com.habify.entity.HabitLog;
import com.habify.entity.HabitStatus;
import com.habify.exception.BadRequestException;
import com.habify.repository.GoalRepository;
import com.habify.repository.HabitLogRepository;
import com.habify.repository.HabitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HabitLogService
 * 
 * Core daily activity tracking engine.
 * Handles daily progress recording, status updates, validation against future dates
 * and inactive habits, and graceful handling of single-log-per-day constraints.
 */
@Service
public class HabitLogService {

    private final HabitLogRepository habitLogRepository;
    private final HabitRepository habitRepository;
    private final HabitService habitService;
    private final StreakService streakService;

    @Autowired(required = false)
    private GoalRepository goalRepository;

    public HabitLogService(HabitLogRepository habitLogRepository,
                           HabitRepository habitRepository,
                           HabitService habitService,
                           StreakService streakService) {
        this(habitLogRepository, habitRepository, habitService, streakService, null);
    }

    @Autowired
    public HabitLogService(HabitLogRepository habitLogRepository,
                           HabitRepository habitRepository,
                           HabitService habitService,
                           StreakService streakService,
                           @Autowired(required = false) GoalRepository goalRepository) {
        this.habitLogRepository = habitLogRepository;
        this.habitRepository = habitRepository;
        this.habitService = habitService;
        this.streakService = streakService;
        this.goalRepository = goalRepository;
    }

    /**
     * Records or updates a daily tracking log for a habit.
     */
    @Transactional
    public HabitLogResponse logHabit(Long userId, Long habitId, HabitLogRequest request) {
        Habit habit = habitService.getHabitEntityByIdAndUser(habitId, userId);

        if (!Boolean.TRUE.equals(habit.getIsActive())) {
            throw new BadRequestException("Cannot log activity for an inactive habit. Please activate it first.");
        }

        LocalDate today = LocalDate.now();
        LocalDate logDate = request.getLogDate() != null ? request.getLogDate() : today;

        if (logDate.isAfter(today)) {
            throw new BadRequestException("Cannot log habits for future dates (" + logDate + ").");
        }

        // Enforce scheduling rules server-side: reject non-scheduled days
        if (!habit.isScheduledFor(logDate)) {
            throw new BadRequestException("Cannot log activity on a non-scheduled day (" + logDate.getDayOfWeek() + "). Rest days cannot be logged as activities.");
        }

        // Determine status and percentage from actualValue vs targetValue, or explicit status
        HabitStatus status = request.getStatus();
        int percentage;
        Double actualValue = request.getActualValue();

        if (actualValue != null && habit.getTargetValue() != null && habit.getTargetValue() > 0) {
            percentage = (int) Math.round((actualValue / habit.getTargetValue()) * 100);
            percentage = Math.min(100, Math.max(0, percentage));
            if (status == null) {
                if (percentage >= 100) {
                    status = HabitStatus.COMPLETED;
                } else if (percentage > 0) {
                    status = HabitStatus.PARTIAL;
                } else {
                    status = HabitStatus.MISSED;
                }
            }
        } else {
            if (status == null) {
                status = HabitStatus.COMPLETED;
            }
            if (status == HabitStatus.COMPLETED) {
                percentage = 100;
                if (actualValue == null && habit.getTargetValue() != null) {
                    actualValue = habit.getTargetValue().doubleValue();
                }
            } else if (status == HabitStatus.MISSED) {
                percentage = 0;
                if (actualValue == null) {
                    actualValue = 0.0;
                }
            } else {
                // PARTIAL
                if (request.getCompletionPercentage() != null) {
                    percentage = Math.min(100, Math.max(1, request.getCompletionPercentage()));
                } else {
                    percentage = 50; // default partial percentage
                }
                if (actualValue == null && habit.getTargetValue() != null) {
                    actualValue = Math.round((habit.getTargetValue() * (percentage / 100.0)) * 100.0) / 100.0;
                }
            }
        }

        // Check if log already exists for this date
        Optional<HabitLog> existingOpt = habitLogRepository.findByHabitIdAndLogDate(habitId, logDate);
        HabitLog log;

        if (existingOpt.isPresent()) {
            log = existingOpt.get();
            log.setStatus(status);
            log.setCompletionPercentage(percentage);
            log.setActualValue(actualValue);
            log.setCompletionTime(LocalTime.now());
            if (request.getNote() != null) {
                log.setNote(request.getNote().trim());
            }
        } else {
            log = new HabitLog();
            log.setHabit(habit);
            log.setLogDate(logDate);
            log.setStatus(status);
            log.setCompletionPercentage(percentage);
            log.setActualValue(actualValue);
            log.setCompletionTime(LocalTime.now());
            log.setNote(request.getNote() != null ? request.getNote().trim() : null);
        }

        boolean newlyCompleted = (status == HabitStatus.COMPLETED) && (existingOpt.isEmpty() || existingOpt.get().getStatus() != HabitStatus.COMPLETED);

        HabitLog savedLog = habitLogRepository.save(log);

        if (newlyCompleted && goalRepository != null) {
            updateLinkedGoalsOnCompletion(userId);
        }

        return mapEntityToResponse(savedLog);
    }

    private void updateLinkedGoalsOnCompletion(Long userId) {
        try {
            List<Goal> activeGoals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);
            for (Goal g : activeGoals) {
                if ("HABIT_LOGS".equalsIgnoreCase(g.getTargetType()) || g.getTargetType() == null) {
                    int nextVal = (g.getCurrentValue() != null ? g.getCurrentValue() : 0) + 1;
                    g.setCurrentValue(nextVal);
                    if (g.getTargetValue() != null && nextVal >= g.getTargetValue()) {
                        g.setStatus(GoalStatus.COMPLETED);
                    }
                    goalRepository.save(g);
                }
            }
        } catch (Exception ignored) {
            // Non-blocking goal update
        }
    }

    /**
     * Retrieves all active habits for today with their tracking status.
     */
    public List<TodayHabitDTO> getTodayHabits(Long userId) {
        LocalDate today = LocalDate.now();
        List<Habit> activeHabits = habitRepository.findByUserIdAndIsActiveTrue(userId);

        List<HabitLog> todayLogs = habitLogRepository.findByUserIdAndLogDate(userId, today);
        Map<Long, HabitLog> logByHabitId = todayLogs.stream()
                .collect(Collectors.toMap(l -> l.getHabit().getId(), l -> l, (a, b) -> a));

        List<TodayHabitDTO> dtos = new ArrayList<>();
        for (Habit habit : activeHabits) {
            TodayHabitDTO dto = new TodayHabitDTO();
            dto.setHabitId(habit.getId());
            dto.setTitle(habit.getTitle());
            dto.setCategory(habit.getCategory());
            dto.setPriority(habit.getPriority());
            dto.setPreferredTime(habit.getPreferredTime());
            dto.setTargetValue(habit.getTargetValue());
            dto.setUnit(habit.getUnit());
            dto.setScheduledToday(habit.isScheduledFor(today));

            HabitLog log = logByHabitId.get(habit.getId());
            if (log != null) {
                dto.setLogId(log.getId());
                dto.setStatus(log.getStatus());
                dto.setCompletionPercentage(log.getCompletionPercentage());
                dto.setActualValue(log.getActualValue());
                dto.setNote(log.getNote());
            }

            dto.setCurrentStreak(streakService.calculateStreak(habit).getCurrentStreak());
            dtos.add(dto);
        }

        return dtos;
    }

    /**
     * Retrieves logs history for a specific habit.
     */
    public List<HabitLogResponse> getLogsByHabit(Long userId, Long habitId) {
        // Verify ownership
        habitService.getHabitEntityByIdAndUser(habitId, userId);

        return habitLogRepository.findByHabitIdOrderByLogDateDesc(habitId)
                .stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves logs for all habits of a user within a specified date range.
     */
    public List<HabitLogResponse> getLogsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return habitLogRepository.findByUserIdAndLogDateBetween(userId, startDate, endDate)
                .stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    public HabitLogResponse mapEntityToResponse(HabitLog log) {
        HabitLogResponse res = new HabitLogResponse();
        res.setId(log.getId());
        res.setHabitId(log.getHabit().getId());
        res.setHabitTitle(log.getHabit().getTitle());
        res.setLogDate(log.getLogDate());
        res.setStatus(log.getStatus());
        res.setCompletionPercentage(log.getCompletionPercentage());
        res.setActualValue(log.getActualValue());
        res.setCompletionTime(log.getCompletionTime());
        res.setNote(log.getNote());
        res.setCreatedAt(log.getCreatedAt());
        return res;
    }
}
