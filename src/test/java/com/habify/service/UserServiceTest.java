package com.habify.service;

import com.habify.dto.AuthResponse;
import com.habify.dto.LoginRequest;
import com.habify.dto.RegisterRequest;
import com.habify.dto.UserProfileResponse;
import com.habify.entity.User;
import com.habify.exception.DuplicateResourceException;
import com.habify.exception.UnauthorizedException;
import com.habify.repository.HabitRepository;
import com.habify.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, habitRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Register: Successfully creates new user account")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("Pooja Verma", "pooja@bca.edu", "pooja_v", "password123");

        when(userRepository.existsByEmail("pooja@bca.edu")).thenReturn(false);
        when(userRepository.existsByUsername("pooja_v")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        User savedUser = new User("Pooja Verma", "pooja@bca.edu", "pooja_v", "hashedPassword");
        savedUser.setId(10L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals(10L, response.getUserId());
        assertEquals("pooja@bca.edu", response.getEmail());
        assertTrue(response.getToken().startsWith("hbf_10_"));
    }

    @Test
    @DisplayName("Register: Duplicate email throws DuplicateResourceException")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Pooja Verma", "existing@bca.edu", "pooja_v", "password123");

        when(userRepository.existsByEmail("existing@bca.edu")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.register(request));
    }

    @Test
    @DisplayName("Login: Valid credentials returns AuthResponse")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("pooja_v", "password123");

        User user = new User("Pooja Verma", "pooja@bca.edu", "pooja_v", "hashedPassword");
        user.setId(10L);

        when(userRepository.findByEmailOrUsername("pooja_v", "pooja_v")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        AuthResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals(10L, response.getUserId());
        assertEquals("pooja_v", response.getUsername());
    }

    @Test
    @DisplayName("Login: Invalid password throws UnauthorizedException")
    void testLoginInvalidPassword() {
        LoginRequest request = new LoginRequest("pooja_v", "wrongpassword");

        User user = new User("Pooja Verma", "pooja@bca.edu", "pooja_v", "hashedPassword");
        user.setId(10L);

        when(userRepository.findByEmailOrUsername("pooja_v", "pooja_v")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("GetProfile: Successfully retrieves user profile and habit counts")
    void testGetProfileSuccess() {
        Long userId = 10L;
        User user = new User("Pooja Verma", "pooja@bca.edu", "pooja_v", "hashedPassword");
        user.setId(userId);
        user.setCreatedAt(LocalDateTime.now().minusDays(5));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(habitRepository.countByUserId(userId)).thenReturn(4L);
        when(habitRepository.countByUserIdAndIsActiveTrue(userId)).thenReturn(3L);

        UserProfileResponse profile = userService.getProfile(userId);

        assertNotNull(profile);
        assertEquals(userId, profile.getId());
        assertEquals("Pooja Verma", profile.getFullName());
        assertEquals("pooja@bca.edu", profile.getEmail());
        assertEquals(4L, profile.getTotalHabits());
        assertEquals(3L, profile.getActiveHabits());
    }
}
