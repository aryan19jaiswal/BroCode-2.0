package com.broCode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "Email or username is required")
    @Size(max = 100, message = "Identifier must be at most 100 characters")
    private String identifier;

    @NotBlank(message = "Password is required")
    @Size(max = 128, message = "Password must be at most 128 characters")
    private String password;
}
