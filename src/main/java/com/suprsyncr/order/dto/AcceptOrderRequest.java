package com.suprsyncr.order.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for accepting an order.
 * Requires warehouse ID to reserve inventory from.
 * 
 * Requirements: 16, 92
 */
public record AcceptOrderRequest(
    @NotNull(message = "Warehouse ID is required")
    Long warehouseId
) {}

