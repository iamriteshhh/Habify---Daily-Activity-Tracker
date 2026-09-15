package com.habify.service;

import com.habify.dto.*;
import com.habify.entity.User;
import com.habify.exception.BadRequestException;
import com.habify.exception.DuplicateResourceException;
import com.habify.exception.ResourceNotFoundException;
import com.habify.exception.UnauthorizedException;
import com.habify.repository.HabitRepository;
import com.habify.repository.UserRepository;
import com.habify.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * UserService
 * 
 * Handles user account registration, JWT authentication, email verification,
 * secure password resets, and user profile management.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final HabitRepository habitRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MailService mailService;

    public UserService(UserRepository userRepository,
                       HabitRepository habitRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.habitRepository = habitRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.mailService = mailService;
    }

    /**
     * Registers a new user with email verification token.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with email '" + email + "' already exists.");
        }

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username '" + username + "' is already taken.");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getFullName().trim(), email, username, hashedPassword);
        
        // Generate email verification token (valid for 48 hours)
        String verifyToken = UUID.randomUUID().toString().replace("-", "");
        user.setVerificationToken(verifyToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(48));
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        // Dispatch verification email / log action
        mailService.sendVerificationEmail(savedUser.getEmail(), verifyToken);

        // Issue signed JWT
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getUsername(),
                token,
                "Registration successful! Please verify your email via the link sent to your inbox.",
                savedUser.getEmailVerified()
        );
    }

    /**
     * Authenticates user credentials and issues a signed JWT.
     */
    public AuthResponse login(LoginRequest request) {
        String loginId = request.getEmailOrUsername().trim().toLowerCase();
        User user = userRepository.findByEmailOrUsername(loginId, loginId)
                .orElseThrow(() -> new UnauthorizedException("Invalid email/username or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email/username or password.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getEmail());

        return new AuthResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUsername(),
                token,
                "Login successful! Welcome back.",
                Boolean.TRUE.equals(user.getEmailVerified())
        );
    }

    /**
     * Validates email verification token and marks account as verified.
     */
    @Transactional
    public MessageResponse verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BadRequestException("Verification token is required.");
        }

        User user = userRepository.findByVerificationToken(token.trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token."));

        if (user.getVerificationTokenExpiry() != null && user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification token has expired. Please request a new one.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        return MessageResponse.ok("Email verified successfully! You have full access to your Habify workspace.");
    }

    /**
     * Initiates password reset flow by sending a 15-minute token.
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String resetToken = UUID.randomUUID().toString().replace("-", "");
            user.setResetPasswordToken(resetToken);
            user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);

            mailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        }

        // Return constant message to prevent account enumeration
        return MessageResponse.ok("If an account with this email exists, password reset instructions have been sent.");
    }

    /**
     * Resets user password using a single-use token.
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken().trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token."));

        if (user.getResetPasswordTokenExpiry() != null && user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        return MessageResponse.ok("Password has been successfully updated. You can now log in with your new password.");
    }

    /**
     * Retrieves user profile details and habit counts.
     */
    public UserProfileResponse getProfile(Long userId) {
        User user = getUserById(userId);
        long totalHabits = habitRepository.countByUserId(userId);
        long activeHabits = habitRepository.countByUserIdAndIsActiveTrue(userId);

        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUsername(),
                totalHabits,
                activeHabits,
                Boolean.TRUE.equals(user.getEmailVerified()),
                user.getCreatedAt()
        );
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }
}
