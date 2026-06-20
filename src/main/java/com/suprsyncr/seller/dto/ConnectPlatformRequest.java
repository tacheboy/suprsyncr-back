package com.suprsyncr.seller.dto;

import com.suprsyncr.seller.entity.PlatformType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for connecting an existing platform account.
 */
public record ConnectPlatformRequest(
    @NotNull(message = "Platform type is required")
    PlatformType platformType,
    
    @NotBlank(message = "Store name is required")
    String storeName,
    
    @NotNull(message = "Credentials are required")
    Map<String, String> credentials
) {}

