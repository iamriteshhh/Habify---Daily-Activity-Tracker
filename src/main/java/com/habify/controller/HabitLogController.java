package com.habify.controller;

import com.habify.config.AuthHelper;
import com.habify.dto.HabitLogRequest;
import com.habify.dto.HabitLogResponse;
import com.habify.dto.TodayHabitDTO;
import com.habify.service.HabitLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

/**
 * HabitLogController
 * 
 * REST API for daily activity tracking logs.
 */
@RestController
@RequestMapping("/api")
public class HabitLogController {

    private final HabitLogService habitLogService;
    private final AuthHelper authHelper;

    public HabitLogController(HabitLogService habitLogService, AuthHelper authHelper) {
        this.habitLogService = habitLogService;
        this.authHelper = authHelper;
    }

    @PostMapping("/habits/{habitId}/logs")
    public ResponseEntity<HabitLogResponse> logHabit(@PathVariable("habitId") Long habitId,
                                                     @Valid @RequestBody HabitLogRequest logRequest,
                                                     HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        HabitLogResponse response = habitLogService.logHabit(userId, habitId, logRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/habits/{habitId}/logs")
    public ResponseEntity<List<HabitLogResponse>> getHabitLogs(@PathVariable("habitId") Long habitId,
                                                               HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        List<HabitLogResponse> logs = habitLogService.getLogsByHabit(userId, habitId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/today")
    public ResponseEntity<List<TodayHabitDTO>> getTodayLogs(HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        List<TodayHabitDTO> todayHabits = habitLogService.getTodayHabits(userId);
        return ResponseEntity.ok(todayHabits);
    }

    @GetMapping("/logs/range")
    public ResponseEntity<List<HabitLogResponse>> getLogsByRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        List<HabitLogResponse> logs = habitLogService.getLogsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(logs);
    }
}
