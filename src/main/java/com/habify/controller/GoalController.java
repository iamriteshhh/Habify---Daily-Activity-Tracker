package com.habify.controller;

import com.habify.config.AuthHelper;
import com.habify.dto.GoalRequest;
import com.habify.dto.GoalResponse;
import com.habify.service.GoalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;
    private final AuthHelper authHelper;

    public GoalController(GoalService goalService, AuthHelper authHelper) {
        this.goalService = goalService;
        this.authHelper = authHelper;
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getGoals(HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        return ResponseEntity.ok(goalService.getGoalsByUser(userId));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest goalRequest,
                                                   HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        GoalResponse response = goalService.createGoal(userId, goalRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/increment")
    public ResponseEntity<GoalResponse> incrementProgress(@PathVariable("id") Long id,
                                                          @RequestParam(name = "amount", defaultValue = "1") int amount,
                                                          HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        GoalResponse response = goalService.incrementProgress(userId, id, amount);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteGoal(@PathVariable("id") Long id,
                                                          HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        goalService.deleteGoal(userId, id);
        return ResponseEntity.ok(Map.of("message", "Goal deleted successfully", "goalId", id.toString()));
    }
}
