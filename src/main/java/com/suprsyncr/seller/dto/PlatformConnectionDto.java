package com.suprsyncr.seller.dto;

import com.suprsyncr.seller.entity.AccountCreationMethod;
import com.suprsyncr.seller.entity.ConnectionStatus;
import com.suprsyncr.seller.entity.PlatformType;

import java.time.LocalDateTime;

/**
 * DTO for platform connection information.
 */
public record PlatformConnectionDto(
    Long id,
    PlatformType platformType,
    String storeName,
    ConnectionStatus status,
    AccountCreationMethod creationMethod,
    String externalStoreId,
    LocalDateTime lastSyncedAt,
    String lastSyncError
) {}

