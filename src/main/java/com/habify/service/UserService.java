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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
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
     * Hashes a sensitive raw token using SHA-256 before database persistence.
     * Protects tokens against theft in the event of database compromise or backup leaks.
     */
    public static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
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
        String rawVerifyToken = UUID.randomUUID().toString().replace("-", "");
        user.setVerificationToken(hashToken(rawVerifyToken));
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(48));
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        // Dispatch verification email / log action with raw token
        mailService.sendVerificationEmail(savedUser.getEmail(), rawVerifyToken);

        // Issue signed JWT
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getTokenVersion());

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

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getEmail(), user.getTokenVersion());

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

        String hashedToken = hashToken(token.trim());
        User user = userRepository.findByVerificationToken(hashedToken)
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
            String rawResetToken = UUID.randomUUID().toString().replace("-", "");
            user.setResetPasswordToken(hashToken(rawResetToken));
            user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);

            mailService.sendPasswordResetEmail(user.getEmail(), rawResetToken);
        }

        // Return constant message to prevent account enumeration
        return MessageResponse.ok("If an account with this email exists, password reset instructions have been sent.");
    }

    /**
     * Resets user password using a single-use token.
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String hashedToken = hashToken(request.getToken().trim());
        User user = userRepository.findByResetPasswordToken(hashedToken)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token."));

        if (user.getResetPasswordTokenExpiry() != null && user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        // Increment token version to invalidate all previously issued tokens
        user.setTokenVersion((user.getTokenVersion() != null ? user.getTokenVersion() : 1) + 1);
        userRepository.save(user);

        return MessageResponse.ok("Password has been successfully updated. You can now log in with your new password.");
    }

    /**
     * Revokes active session tokens for the user upon logout.
     */
    @Transactional
    public void logout(Long userId) {
        User user = getUserById(userId);
        user.setTokenVersion((user.getTokenVersion() != null ? user.getTokenVersion() : 1) + 1);
        userRepository.save(user);
    }

    /**
     * Resends email verification link.
     */
    @Transactional
    public MessageResponse resendVerification(Long userId) {
        User user = getUserById(userId);
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return MessageResponse.ok("Your email is already verified.");
        }

        String rawVerifyToken = UUID.randomUUID().toString().replace("-", "");
        user.setVerificationToken(hashToken(rawVerifyToken));
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(48));
        userRepository.save(user);

        mailService.sendVerificationEmail(user.getEmail(), rawVerifyToken);
        return MessageResponse.ok("A new verification link has been sent to your email.");
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
