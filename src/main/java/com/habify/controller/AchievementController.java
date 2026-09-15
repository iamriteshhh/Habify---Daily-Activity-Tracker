package com.habify.controller;

import com.habify.config.AuthHelper;
import com.habify.dto.AchievementDTO;
import com.habify.service.AchievementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;
    private final AuthHelper authHelper;

    public AchievementController(AchievementService achievementService, AuthHelper authHelper) {
        this.achievementService = achievementService;
        this.authHelper = authHelper;
    }

    @GetMapping
    public ResponseEntity<List<AchievementDTO>> getAchievements(HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        List<AchievementDTO> achievements = achievementService.getUserAchievements(userId);
        return ResponseEntity.ok(achievements);
    }
}
