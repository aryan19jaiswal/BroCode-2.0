package com.broCode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * All fields are optional (PATCH semantics). A null field means "don't change it".
 * Constraints only fire when a field is non-null.
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    private String username;

    @Email(message = "Must be a valid email address")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Size(min = 8, max = 128, message = "Password must be 8–128 characters")
    private String password;
}
