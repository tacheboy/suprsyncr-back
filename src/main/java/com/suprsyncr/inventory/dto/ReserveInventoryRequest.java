package com.suprsyncr.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for reserving inventory.
 * 
 * Requirements: 9, 10, 11, 56, 92
 */
public record ReserveInventoryRequest(
    @NotNull Long productVariantId,
    @NotNull Long warehouseId,
    @NotNull @Min(1) Integer quantity,
    @NotBlank String referenceType,
    @NotNull String referenceId
) {}

