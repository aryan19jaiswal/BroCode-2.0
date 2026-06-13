package com.broCode.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for all REST controllers.
 * Returns consistent JSON error responses and prevents unhandled exceptions
 * from leaking stack traces to the client.
 *
 * Handler resolution order (most specific first):
 *   MethodArgumentNotValidException  → 400  (Bean Validation failure)
 *   DuplicateKeyException            → 409  (MongoDB unique index violation)
 *   DuplicateEmailException          → 409
 *   DuplicateUsernameException       → 409
 *   InvalidCredentialsException      → 401
 *   UserNotFoundException            → 404
 *   InvalidSessionException          → 400
 *   IllegalArgumentException         → 400
 *   RuntimeException                 → 500  (catch-all for unhandled runtime errors)
 *   Exception                        → 500  (catch-all)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return ResponseEntity.badRequest()
                .body(Map.of("message", message, "success", false));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateKey(DuplicateKeyException ex) {
        String raw = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        String field = raw.contains("email") ? "Email"
                : raw.contains("username") ? "Username"
                : "A field";
        log.warn("Duplicate key violation — {}: {}", field, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", field + " is already taken", "success", false));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex) {
        log.warn("Duplicate email: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage(), "success", false));
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateUsername(DuplicateUsernameException ex) {
        log.warn("Duplicate username: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage(), "success", false));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Invalid credentials attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", ex.getMessage(), "success", false));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage(), "success", false));
    }

    @ExceptionHandler(InvalidSessionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSession(InvalidSessionException ex) {
        log.warn("Invalid session: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("message", ex.getMessage(), "success", false));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("message", ex.getMessage(), "success", false));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.error("Unhandled runtime exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An unexpected error occurred", "success", false));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An unexpected error occurred", "success", false));
    }
}
