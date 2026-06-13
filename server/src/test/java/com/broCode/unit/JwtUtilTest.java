package com.broCode.unit;

import com.broCode.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretString",
                "test-secret-key-that-is-at-least-32-chars-long-padding-yes!");
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 3_600_000L);
        jwtUtil.init();
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        String token = jwtUtil.generateToken("user-id-123");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsSubjectSetDuringGeneration() {
        String token = jwtUtil.generateToken("user-id-abc");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user-id-abc");
    }

    @Test
    void extractUserId_isSameAsExtractUsername() {
        String token = jwtUtil.generateToken("user-id-xyz");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(jwtUtil.extractUsername(token));
    }

    @Test
    void validateToken_freshToken_returnsTrue() {
        String token = jwtUtil.generateToken("user-id-123");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        // Set expiration to 0 ms so the token is immediately expired
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 0L);
        String token = jwtUtil.generateToken("user-id-123");
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_tamperedSignature_returnsFalse() {
        String token = jwtUtil.generateToken("user-id-123");
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.jwt")).isFalse();
    }
}
