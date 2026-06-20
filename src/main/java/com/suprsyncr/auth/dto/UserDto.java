package com.suprsyncr.auth.dto;

import com.suprsyncr.auth.entity.UserRole;

/**
 * DTO for user information.
 * Contains public user details without sensitive information.
 */
public record UserDto(
        Long id,
        String email,
        String fullName,
        UserRole role
) {}

