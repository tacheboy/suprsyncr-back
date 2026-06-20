package com.suprsyncr.order.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for shipping an order.
 * Requires tracking number and courier partner information.
 * 
 * Requirements: 17, 92
 */
public record ShipOrderRequest(
    @NotBlank(message = "Tracking number is required")
    String trackingNumber,
    
    @NotBlank(message = "Courier partner is required")
    String courierPartner
) {}

