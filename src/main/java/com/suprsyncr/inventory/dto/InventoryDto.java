package com.suprsyncr.inventory.dto;

/**
 * DTO for inventory information.
 * 
 * Requirements: 9, 10, 11, 56, 92
 */
public record InventoryDto(
    Long id,
    Long productVariantId,
    String variantSku,
    Long warehouseId,
    String warehouseName,
    Integer availableQuantity,
    Integer reservedQuantity,
    Integer totalQuantity,
    Integer lowStockThreshold,
    boolean isLowStock
) {}

