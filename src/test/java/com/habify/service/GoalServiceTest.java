package com.habify.service;

import com.habify.dto.GoalRequest;
import com.habify.dto.GoalResponse;
import com.habify.entity.Goal;
import com.habify.entity.GoalStatus;
import com.habify.entity.User;
import com.habify.exception.ResourceNotFoundException;
import com.habify.repository.GoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserService userService;

    private GoalService goalService;

    private User testUser;

    @BeforeEach
    void setUp() {
        goalService = new GoalService(goalRepository, userService);
        testUser = new User();
        testUser.setId(10L);
        testUser.setUsername("charlie");
        testUser.setEmail("charlie@habify.com");
    }

    @Test
    @DisplayName("Create goal successfully with zero initial progress")
    void testCreateGoal() {
        GoalRequest request = new GoalRequest();
        request.setTitle("Run 50km");
        request.setDescription("Monthly cardio goal");
        request.setTargetValue(50);
        request.setUnit("km");
        request.setDeadline(LocalDate.now().plusDays(30));

        when(userService.getUserById(10L)).thenReturn(testUser);
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
            Goal g = invocation.getArgument(0);
            g.setId(101L);
            return g;
        });

        GoalResponse response = goalService.createGoal(10L, request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("Run 50km", response.getTitle());
        assertEquals(50, response.getTargetValue());
        assertEquals(0, response.getCurrentValue());
        assertEquals(0, response.getProgressPercentage());
        assertEquals(GoalStatus.ACTIVE, response.getStatus());
        verify(goalRepository, times(1)).save(any(Goal.class));
    }

    @Test
    @DisplayName("Increment goal progress and auto-complete when reaching target")
    void testIncrementProgressAutoComplete() {
        Goal goal = new Goal(testUser, "Read 5 Books", "Annual reading", 5, "books", LocalDate.now().plusMonths(2));
        goal.setId(200L);
        goal.setCurrentValue(4);
        goal.setStatus(GoalStatus.ACTIVE);

        when(goalRepository.findByIdAndUserId(200L, 10L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse response = goalService.incrementProgress(10L, 200L, 1);

        assertEquals(5, response.getCurrentValue());
        assertEquals(100, response.getProgressPercentage());
        assertEquals(GoalStatus.COMPLETED, response.getStatus());
    }

    @Test
    @DisplayName("Increment progress throws ResourceNotFoundException for unknown or unauthorized goal")
    void testIncrementNotFound() {
        when(goalRepository.findByIdAndUserId(999L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goalService.incrementProgress(10L, 999L, 1));
    }

    @Test
    @DisplayName("Retrieve goals list for user")
    void testGetGoalsByUser() {
        Goal g1 = new Goal(testUser, "Goal 1", "desc", 10, "times", null);
        g1.setId(1L);
        Goal g2 = new Goal(testUser, "Goal 2", "desc", 20, "times", null);
        g2.setId(2L);

        when(goalRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(g1, g2));

        List<GoalResponse> goals = goalService.getGoalsByUser(10L);

        assertEquals(2, goals.size());
        assertEquals("Goal 1", goals.get(0).getTitle());
        assertEquals("Goal 2", goals.get(1).getTitle());
    }
}
