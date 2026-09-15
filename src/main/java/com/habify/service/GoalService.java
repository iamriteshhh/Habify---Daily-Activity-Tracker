package com.habify.service;

import com.habify.dto.GoalRequest;
import com.habify.dto.GoalResponse;
import com.habify.entity.Goal;
import com.habify.entity.GoalStatus;
import com.habify.entity.User;
import com.habify.exception.ResourceNotFoundException;
import com.habify.repository.GoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GoalService
 * 
 * Manages target milestones, tracks completion criteria, and auto-transitions
 * statuses between ACTIVE, COMPLETED, and EXPIRED.
 */
@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserService userService;

    public GoalService(GoalRepository goalRepository, UserService userService) {
        this.goalRepository = goalRepository;
        this.userService = userService;
    }

    /**
     * Retrieves all goals configured by the user.
     */
    public List<GoalResponse> getGoalsByUser(Long userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::checkAndMap)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new target goal.
     */
    @Transactional
    public GoalResponse createGoal(Long userId, GoalRequest request) {
        User user = userService.getUserById(userId);

        Goal goal = new Goal(
                user,
                request.getTitle().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getTargetValue(),
                request.getUnit() != null ? request.getUnit().trim() : "times",
                request.getDeadline()
        );

        Goal saved = goalRepository.save(goal);
        return checkAndMap(saved);
    }

    /**
     * Increments progress for an active goal.
     */
    @Transactional
    public GoalResponse incrementProgress(Long userId, Long goalId, int amount) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with ID: " + goalId));

        if (goal.getStatus() != GoalStatus.EXPIRED) {
            int newCurrent = Math.max(0, goal.getCurrentValue() + amount);
            goal.setCurrentValue(newCurrent);

            if (goal.getCurrentValue() >= goal.getTargetValue()) {
                goal.setStatus(GoalStatus.COMPLETED);
            } else {
                goal.setStatus(GoalStatus.ACTIVE);
            }
        }

        Goal saved = goalRepository.save(goal);
        return checkAndMap(saved);
    }

    /**
     * Deletes a goal permanently.
     */
    @Transactional
    public void deleteGoal(Long userId, Long goalId) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with ID: " + goalId));
        goalRepository.delete(goal);
    }

    private GoalResponse checkAndMap(Goal goal) {
        LocalDate today = LocalDate.now();
        if (goal.getStatus() == GoalStatus.ACTIVE && goal.getDeadline() != null && goal.getDeadline().isBefore(today)) {
            if (goal.getCurrentValue() >= goal.getTargetValue()) {
                goal.setStatus(GoalStatus.COMPLETED);
            } else {
                goal.setStatus(GoalStatus.EXPIRED);
            }
            goalRepository.save(goal);
        }

        int pct = 0;
        if (goal.getTargetValue() != null && goal.getTargetValue() > 0) {
            pct = Math.min(100, Math.round(((float) goal.getCurrentValue() / goal.getTargetValue()) * 100));
        }

        return new GoalResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getTargetType(),
                goal.getTargetValue(),
                goal.getCurrentValue(),
                pct,
                goal.getUnit(),
                goal.getDeadline(),
                goal.getStatus(),
                goal.getCreatedAt()
        );
    }
}
