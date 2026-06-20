package com.suprsyncr.integration.connector;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Record representing an order fetched from an external marketplace platform.
 * Used to transfer order data from marketplace APIs to the USP system.
 */
public record ExternalOrder(
    String externalOrderId,
    BigDecimal totalAmount,
    String customerName,
    String shippingAddress,
    String customerPhone,
    String customerEmail,
    LocalDateTime orderedAt,
    List<ExternalOrderItem> items
) {}
