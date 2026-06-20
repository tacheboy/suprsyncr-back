package com.suprsyncr.order.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for cancelling an order.
 * Requires a reason for the cancellation.
 * 
 * Requirements: 18, 92
 */
public record CancelOrderRequest(
    @NotBlank(message = "Cancellation reason is required")
    String reason
) {}

