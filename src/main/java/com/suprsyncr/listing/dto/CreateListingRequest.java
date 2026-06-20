package com.suprsyncr.listing.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for creating product listings across multiple platforms.
 * Supports bulk listing creation by specifying multiple platform IDs.
 */
public record CreateListingRequest(
    @NotNull Long productId,
    @NotEmpty List<Long> platformIds
) {}

