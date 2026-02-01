package com.payflow.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm",
                900000,   // 15 min
                604800000 // 7 days
        );
    }

    @Test
    void generateAndValidateAccessToken() {
        String token = jwtUtil.generateAccessToken(1L, "test@example.com");

        assertThat(jwtUtil.isValid(token)).isTrue();
        assertThat(jwtUtil.getUserId(token)).isEqualTo("1");
        assertThat(jwtUtil.getTokenType(token)).isEqualTo("access");
    }

    @Test
    void generateAndValidateRefreshToken() {
        String token = jwtUtil.generateRefreshToken(1L, "test@example.com");

        assertThat(jwtUtil.isValid(token)).isTrue();
        assertThat(jwtUtil.getTokenType(token)).isEqualTo("refresh");
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertThat(jwtUtil.isValid("not-a-valid-token")).isFalse();
    }

    @Test
    void parseTokenContainsClaims() {
        String token = jwtUtil.generateAccessToken(42L, "user@example.com");
        Claims claims = jwtUtil.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
    }
}
