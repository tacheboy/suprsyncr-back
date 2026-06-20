package com.suprsyncr.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing access tokens.
 * Contains the refresh token to be validated.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}

