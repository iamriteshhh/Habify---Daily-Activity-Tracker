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

    @Mock
    private com.habify.security.JwtUtil jwtUtil;

    @Mock
    private MailService mailService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, habitRepository, passwordEncoder, jwtUtil, mailService);
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
        when(jwtUtil.generateToken(10L, "pooja_v", "pooja@bca.edu", 1)).thenReturn("jwt.signed.token");

        AuthResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals(10L, response.getUserId());
        assertEquals("pooja@bca.edu", response.getEmail());
        assertEquals("jwt.signed.token", response.getToken());
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
        when(jwtUtil.generateToken(10L, "pooja_v", "pooja@bca.edu", 1)).thenReturn("jwt.signed.token");

        AuthResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals(10L, response.getUserId());
        assertEquals("pooja_v", response.getUsername());
        assertEquals("jwt.signed.token", response.getToken());
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

    @Test
    @DisplayName("ResetPassword: Increments tokenVersion and validates against SHA-256 hashed token")
    void testResetPasswordIncrementsTokenVersion() {
        User user = new User("Pooja Verma", "pooja@bca.edu", "pooja_v", "oldHash");
        user.setTokenVersion(1);
        String rawToken = "validToken123";
        String hashedToken = UserService.hashToken(rawToken);
        user.setResetPasswordToken(hashedToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByResetPasswordToken(hashedToken)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHash");

        com.habify.dto.ResetPasswordRequest request = new com.habify.dto.ResetPasswordRequest(rawToken, "newPassword123");
        com.habify.dto.MessageResponse response = userService.resetPassword(request);

        assertNotNull(response);
        assertEquals(2, user.getTokenVersion());
        assertNull(user.getResetPasswordToken());
        assertNull(user.getResetPasswordTokenExpiry());
    }

    @Test
    @DisplayName("VerifyEmail: Validates SHA-256 hashed token and marks account as verified")
    void testVerifyEmailSuccess() {
        User user = new User("Pooja Verma", "pooja@bca.edu", "pooja_v", "hashedPassword");
        String rawToken = "rawVerifyToken123";
        String hashedToken = UserService.hashToken(rawToken);
        user.setVerificationToken(hashedToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        user.setEmailVerified(false);

        when(userRepository.findByVerificationToken(hashedToken)).thenReturn(Optional.of(user));

        com.habify.dto.MessageResponse response = userService.verifyEmail(rawToken);

        assertNotNull(response);
        assertTrue(user.getEmailVerified());
        assertNull(user.getVerificationToken());
        assertNull(user.getVerificationTokenExpiry());
    }

    @Test
    @DisplayName("VerifyEmail: Expired verification token throws BadRequestException")
    void testVerifyEmailExpired() {
        User user = new User("Pooja Verma", "pooja@bca.edu", "pooja_v", "hashedPassword");
        String rawToken = "expiredToken123";
        String hashedToken = UserService.hashToken(rawToken);
        user.setVerificationToken(hashedToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().minusHours(1));

        when(userRepository.findByVerificationToken(hashedToken)).thenReturn(Optional.of(user));

        assertThrows(com.habify.exception.BadRequestException.class, () -> userService.verifyEmail(rawToken));
    }

    @Test
    @DisplayName("ForgotPassword: Hashes reset token and dispatches email with raw token")
    void testForgotPasswordDispatchesRawToken() {
        User user = new User("Pooja Verma", "pooja@bca.edu", "pooja_v", "hashedPassword");
        when(userRepository.findByEmail("pooja@bca.edu")).thenReturn(Optional.of(user));

        com.habify.dto.ForgotPasswordRequest request = new com.habify.dto.ForgotPasswordRequest("pooja@bca.edu");
        com.habify.dto.MessageResponse response = userService.forgotPassword(request);

        assertNotNull(response);
        assertNotNull(user.getResetPasswordToken());
        // Verify token saved in entity is a 64-character SHA-256 hex string
        assertEquals(64, user.getResetPasswordToken().length());
        assertNotNull(user.getResetPasswordTokenExpiry());
    }

    @Test
    @DisplayName("Logout: Increments tokenVersion to revoke active tokens")
    void testLogoutRevokesTokens() {
        User user = new User("Pooja Verma", "pooja@bca.edu", "pooja_v", "hash");
        user.setId(10L);
        user.setTokenVersion(3);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        userService.logout(10L);

        assertEquals(4, user.getTokenVersion());
    }
}
