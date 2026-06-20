package com.suprsyncr.order.dto;

import java.math.BigDecimal;

/**
 * DTO for order item information.
 * Represents a single product item within an order.
 * 
 * Requirements: 15, 92
 */
public record OrderItemDto(
    Long id,
    Long productVariantId,
    String productName,
    String variantName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice
) {}

