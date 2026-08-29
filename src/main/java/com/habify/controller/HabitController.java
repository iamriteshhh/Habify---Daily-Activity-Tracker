package com.habify.controller;

import com.habify.config.AuthHelper;
import com.habify.dto.HabitRequest;
import com.habify.dto.HabitResponse;
import com.habify.service.HabitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * HabitController
 * 
 * REST API for Habit Management (CRUD and active toggling).
 */
@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;
    private final AuthHelper authHelper;

    public HabitController(HabitService habitService, AuthHelper authHelper) {
        this.habitService = habitService;
        this.authHelper = authHelper;
    }

    @GetMapping
    public ResponseEntity<List<HabitResponse>> getAllHabits(HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        List<HabitResponse> habits = habitService.getHabitsByUser(userId);
        return ResponseEntity.ok(habits);
    }

    @PostMapping
    public ResponseEntity<HabitResponse> createHabit(@Valid @RequestBody HabitRequest habitRequest,
                                                     HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        HabitResponse response = habitService.createHabit(userId, habitRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HabitResponse> getHabitById(@PathVariable("id") Long id,
                                                      HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        HabitResponse response = habitService.getHabitResponseById(id, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabitResponse> updateHabit(@PathVariable("id") Long id,
                                                     @Valid @RequestBody HabitRequest habitRequest,
                                                     HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        HabitResponse response = habitService.updateHabit(id, userId, habitRequest);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<HabitResponse> toggleHabitActive(@PathVariable("id") Long id,
                                                           HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        HabitResponse response = habitService.toggleHabitActive(id, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteHabit(@PathVariable("id") Long id,
                                                           HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        habitService.deleteHabit(id, userId);
        return ResponseEntity.ok(Map.of("message", "Habit deleted successfully", "habitId", id.toString()));
    }
}
