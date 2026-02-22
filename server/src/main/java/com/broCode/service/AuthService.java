package com.broCode.service;

import com.broCode.dto.AuthRequest;
import com.broCode.dto.UserDto;
import com.broCode.model.User;
import com.broCode.repository.UserRepository;
import com.broCode.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for handling Authentication and User Profile logic.
 */
@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void register(UserDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
    }

    /**
     * Authenticates a user and returns the JWT + username.
     */
    public record LoginResult(String token, String username) {}

    public LoginResult login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getIdentifier())
                .or(() -> userRepository.findByUsername(request.getIdentifier()))
                .orElseThrow(() -> new RuntimeException("Incorrect email/username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect email/username or password");
        }

        String token = jwtUtil.generateToken(user.getId());
        return new LoginResult(token, user.getUsername());
    }

    /**
     * Returns the username for a given userId.
     */
    public String getUsername(String userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse(null);
    }

    /**
     * Updates an existing user's details.
     * Validates uniqueness for email/username and re-hashes password if changed.
     */
    public void updateUser(String userId, UserDto updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updates.getUsername() != null && !updates.getUsername().isBlank()) {
            if (!user.getUsername().equals(updates.getUsername()) && userRepository.existsByUsername(updates.getUsername())) {
                throw new RuntimeException("Username already taken");
            }
            user.setUsername(updates.getUsername());
        }

        if (updates.getEmail() != null && !updates.getEmail().isBlank()) {
            if (!user.getEmail().equals(updates.getEmail()) && userRepository.existsByEmail(updates.getEmail())) {
                throw new RuntimeException("Email already registered");
            }
            user.setEmail(updates.getEmail());
        }

        if (updates.getPassword() != null && !updates.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updates.getPassword()));
        }

        userRepository.save(user);
    }
}
