package com.suprsyncr.order.dto;

import com.suprsyncr.order.entity.OrderStatus;
import com.suprsyncr.seller.entity.PlatformType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for order response.
 * Contains complete order information including items and timestamps.
 * 
 * Requirements: 15, 16, 17, 18, 19, 20, 92
 */
public record OrderDto(
    Long id,
    String externalOrderId,
    String uspOrderId,
    PlatformType platformType,
    OrderStatus status,
    BigDecimal totalAmount,
    String customerName,
    String shippingAddress,
    String customerPhone,
    String customerEmail,
    LocalDateTime orderedAt,
    LocalDateTime acceptedAt,
    LocalDateTime shippedAt,
    LocalDateTime deliveredAt,
    LocalDateTime cancelledAt,
    String cancellationReason,
    String trackingNumber,
    String courierPartner,
    List<OrderItemDto> items,
    LocalDateTime createdAt
) {}

