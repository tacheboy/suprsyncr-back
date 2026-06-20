package com.suprsyncr.seller.dto;

import com.suprsyncr.seller.entity.PlatformType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a new platform account automatically.
 */
public record CreatePlatformAccountRequest(
    @NotNull(message = "Platform type is required")
    PlatformType platformType,
    
    @NotBlank(message = "Store name is required")
    String storeName
) {}

