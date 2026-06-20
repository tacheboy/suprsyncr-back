package com.suprsyncr.order.dto;

import com.suprsyncr.order.entity.EventTrigger;
import com.suprsyncr.order.entity.OrderStatus;

import java.time.LocalDateTime;

/**
 * DTO for order event information.
 * Represents a status transition or action in the order lifecycle.
 * 
 * Requirements: 54, 92
 */
public record OrderEventDto(
    Long id,
    OrderStatus fromStatus,
    OrderStatus toStatus,
    String notes,
    EventTrigger triggeredBy,
    LocalDateTime createdAt
) {}

