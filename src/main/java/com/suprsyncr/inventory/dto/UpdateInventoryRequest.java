package com.suprsyncr.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating inventory levels.
 * 
 * Requirements: 9, 10, 11, 56, 92
 */
public record UpdateInventoryRequest(
    @NotNull Long productVariantId,
    @NotNull Long warehouseId,
    @NotNull @Min(0) Integer quantity,
    String notes
) {}

