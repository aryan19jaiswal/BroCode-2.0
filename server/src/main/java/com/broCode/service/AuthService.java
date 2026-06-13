package com.broCode.service;

import com.broCode.dto.AuthRequest;
import com.broCode.dto.RegisterRequest;
import com.broCode.dto.UpdateProfileRequest;
import com.broCode.exception.InvalidCredentialsException;
import com.broCode.exception.UserNotFoundException;
import com.broCode.model.User;
import com.broCode.repository.UserRepository;
import com.broCode.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for handling Authentication and User Profile logic.
 *
 * Uniqueness is enforced at the database layer via MongoDB unique indexes on
 * User.email and User.username. A DuplicateKeyException propagates up and is
 * mapped to HTTP 409 by GlobalExceptionHandler — no TOCTOU-prone pre-checks.
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

    public void register(RegisterRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

    public record LoginResult(String token, String username) {}

    public LoginResult login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getIdentifier())
                .or(() -> userRepository.findByUsername(request.getIdentifier()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtUtil.generateToken(user.getId());
        return new LoginResult(token, user.getUsername());
    }

    public String getUsername(String userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Updates an existing user's details.
     * Null or blank fields are skipped (PATCH semantics).
     * Uniqueness violations propagate as DuplicateKeyException → HTTP 409.
     */
    public void updateUser(String userId, UpdateProfileRequest updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (updates.getUsername() != null && !updates.getUsername().isBlank()) {
            user.setUsername(updates.getUsername());
        }
        if (updates.getEmail() != null && !updates.getEmail().isBlank()) {
            user.setEmail(updates.getEmail());
        }
        if (updates.getPassword() != null && !updates.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updates.getPassword()));
        }

        userRepository.save(user);
    }
}
