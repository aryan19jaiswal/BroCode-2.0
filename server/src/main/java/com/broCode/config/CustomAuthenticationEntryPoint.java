package com.broCode.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Handles unauthenticated requests by returning a clean 401 JSON response.
 * Prevents Spring Security from redirecting or throwing "response already committed" errors
 * — especially critical for SSE streaming endpoints where the response must not be partially
 * written before the security check completes.
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Unauthenticated request to {} — {}", request.getRequestURI(), authException.getMessage());

        if (response.isCommitted()) {
            log.warn("Response already committed for {} — cannot send 401", request.getRequestURI());
            return;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        MAPPER.writeValue(response.getOutputStream(),
                Map.of("message", "Authentication required", "success", false));
    }
}
