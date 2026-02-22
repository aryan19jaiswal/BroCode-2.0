package com.broCode.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String identifier; // This maps to email or username
    private String password;
}