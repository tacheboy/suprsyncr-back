package com.suprsyncr.order.dto;

import java.math.BigDecimal;

/**
 * DTO for order statistics.
 * Provides aggregated metrics for order monitoring and business performance.
 * 
 * Requirements: 21, 92
 */
public record OrderStatsDto(
    Long totalOrders,
    Long pendingOrders,
    Long acceptedOrders,
    Long shippedOrders,
    Long deliveredOrders,
    Long cancelledOrders,
    BigDecimal totalRevenue
) {}

