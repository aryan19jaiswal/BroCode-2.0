package com.broCode.controller;

import com.broCode.dto.AuthRequest;
import com.broCode.dto.RegisterRequest;
import com.broCode.dto.UpdateProfileRequest;
import com.broCode.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * Controller for User account operations.
 * Handles registration, login, logout, and profile updates.
 *
 * Exception handling is delegated entirely to GlobalExceptionHandler — no
 * try-catch blocks here. Each service method throws a typed exception that
 * maps to the correct HTTP status code.
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        log.info("New user registered: {}", request.getUsername());
        return ResponseEntity.status(201).body(Map.of("message", "Account created successfully", "success", true));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody AuthRequest authRequest,
                                                     HttpServletResponse response) {
        var result = authService.login(authRequest);

        boolean secure = cookieSecure || "None".equalsIgnoreCase(cookieSameSite);
        ResponseCookie cookie = ResponseCookie.from("token", result.token())
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("User logged in: {}", authRequest.getIdentifier());
        return ResponseEntity.ok(Map.of(
                "message", "Logged in successfully",
                "success", true,
                "username", result.username()
        ));
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = authService.getUsername(userId);
        return ResponseEntity.ok(Map.of("username", username, "success", true));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        boolean secure = cookieSecure || "None".equalsIgnoreCase(cookieSameSite);
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.info("User logged out");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully", "success", true));
    }

    @PatchMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        authService.updateUser(userId, request);
        log.info("User profile updated for ID: {}", userId);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully", "success", true));
    }
}
