package com.suprsyncr.inventory.dto;

import com.suprsyncr.inventory.entity.TransactionType;
import java.time.LocalDateTime;

/**
 * DTO for inventory transaction information.
 * 
 * Requirements: 9, 10, 11, 56, 92
 */
public record InventoryTransactionDto(
    Long id,
    TransactionType type,
    Integer quantity,
    Integer balanceAfter,
    String referenceType,
    String referenceId,
    String notes,
    LocalDateTime createdAt
) {}

