package com.broCode.controller;

import com.broCode.dto.AuthRequest;
import com.broCode.dto.UserDto;
import com.broCode.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for User account operations.
 * Handles registration, login, logout, and profile updates.
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto userDto) {
        try {
            authService.register(userDto);
            log.info("New user registered: {}", userDto.getUsername());
            return ResponseEntity.status(201).body(Map.of("message", "Account created successfully", "success", true));
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "success", false));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest, HttpServletResponse response) {
        try {
            var result = authService.login(authRequest);

            // Set Cookie
            Cookie cookie = new Cookie("token", result.token());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            response.addCookie(cookie);

            log.info("User logged in: {}", authRequest.getIdentifier());
            return ResponseEntity.ok(Map.of(
                    "message", "Logged in successfully",
                    "success", true,
                    "username", result.username()
            ));
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "success", false));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = authService.getUsername(userId);
            return ResponseEntity.ok(Map.of("username", username != null ? username : "", "success", true));
        } catch (Exception e) {
            log.error("Fetch profile failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "success", false));
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        log.info("User logged out");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully", "success", true));
    }

    /**
     * Updates the authenticated user's profile.
     * Extracts the userId from the SecurityContext (set by JwtFilter).
     */
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserDto userDto) {
        try {
            // The Principal is the userId because AuthService.login used user.getId() for the token subject
            String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            authService.updateUser(userId, userDto);

            log.info("User profile updated for ID: {}", userId);
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully", "success", true));
        } catch (Exception e) {
            log.error("Profile update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "success", false));
        }
    }
}
