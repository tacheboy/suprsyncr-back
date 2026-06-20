package com.suprsyncr.listing.dto;

import java.time.LocalDateTime;

/**
 * DTO representing a listing error with detailed information for troubleshooting.
 * Tracks error messages, details, resolution status, and creation timestamp.
 */
public record ListingErrorDto(
    Long id,
    String errorMessage,
    String errorDetails,
    boolean resolved,
    LocalDateTime createdAt
) {}

