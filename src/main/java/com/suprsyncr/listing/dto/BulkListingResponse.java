package com.suprsyncr.listing.dto;

import java.util.List;

/**
 * Response DTO for bulk listing operations.
 * Provides summary statistics and detailed results for each listing attempt.
 */
public record BulkListingResponse(
    int totalRequested,
    int successful,
    int failed,
    List<ListingDto> listings
) {}

