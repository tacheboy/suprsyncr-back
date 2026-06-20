package com.suprsyncr.auth.dto;

/**
 * Response DTO for authentication operations.
 * Contains JWT tokens and user information.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserDto user
) {}

