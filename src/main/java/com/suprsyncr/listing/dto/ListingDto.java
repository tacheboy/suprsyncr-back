package com.suprsyncr.listing.dto;

import com.suprsyncr.listing.entity.ListingStatus;
import com.suprsyncr.seller.entity.PlatformType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a product listing on a marketplace platform.
 * Includes product information, platform details, status, and associated errors.
 */
public record ListingDto(
    Long id,
    Long productId,
    String productName,
    Long platformId,
    PlatformType platformType,
    String externalProductId,
    ListingStatus status,
    LocalDateTime publishedAt,
    LocalDateTime lastSyncedAt,
    List<ListingErrorDto> errors
) {}

