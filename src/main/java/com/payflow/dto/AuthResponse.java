package com.payflow.dto;

public record AuthResponse(String accessToken, String refreshToken, Long userId, String email, String fullName) {}
