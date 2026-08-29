package com.habify.service;

import com.habify.dto.AuthResponse;
import com.habify.dto.LoginRequest;
import com.habify.dto.RegisterRequest;
import com.habify.dto.UserProfileResponse;
import com.habify.entity.User;
import com.habify.exception.DuplicateResourceException;
import com.habify.exception.ResourceNotFoundException;
import com.habify.exception.UnauthorizedException;
import com.habify.repository.HabitRepository;
import com.habify.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * UserService
 * 
 * Handles user account registration, login verification, profile management,
 * and password encryption.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final HabitRepository habitRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, HabitRepository habitRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.habitRepository = habitRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user after verifying email and username uniqueness.
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
        User savedUser = userRepository.save(user);

        String token = generateToken(savedUser);
        return new AuthResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getUsername(),
                token,
                "Registration successful! Welcome to Habify."
        );
    }

    /**
     * Authenticates existing user credentials.
     */
    public AuthResponse login(LoginRequest request) {
        String loginId = request.getEmailOrUsername().trim().toLowerCase();
        User user = userRepository.findByEmailOrUsername(loginId, loginId)
                .orElseThrow(() -> new UnauthorizedException("Invalid email/username or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email/username or password.");
        }

        String token = generateToken(user);
        return new AuthResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUsername(),
                token,
                "Login successful! Welcome back."
        );
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
                user.getCreatedAt()
        );
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    private String generateToken(User user) {
        return "hbf_" + user.getId() + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
