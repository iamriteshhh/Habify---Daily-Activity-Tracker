package com.habify.controller;

import com.habify.config.AuthHelper;
import com.habify.dto.DashboardSummaryResponse;
import com.habify.dto.StreakDTO;
import com.habify.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AnalyticsController
 * 
 * REST API for dashboard summary KPIs, discipline score, and streaks.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AuthHelper authHelper;

    public AnalyticsController(AnalyticsService analyticsService, AuthHelper authHelper) {
        this.analyticsService = analyticsService;
        this.authHelper = authHelper;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        DashboardSummaryResponse summary = analyticsService.getDashboardSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/habits/{habitId}/streak")
    public ResponseEntity<StreakDTO> getHabitStreak(@PathVariable("habitId") Long habitId,
                                                    HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        StreakDTO streak = analyticsService.getHabitStreak(userId, habitId);
        return ResponseEntity.ok(streak);
    }
}
