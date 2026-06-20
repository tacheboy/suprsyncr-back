package com.suprsyncr.integration.connector;

import java.math.BigDecimal;

/**
 * Record representing an order item from an external marketplace platform.
 * Used to transfer order item data from marketplace APIs to the USP system.
 */
public record ExternalOrderItem(
    String externalProductId,
    String productName,
    String variantName,
    Integer quantity,
    BigDecimal unitPrice
) {}
