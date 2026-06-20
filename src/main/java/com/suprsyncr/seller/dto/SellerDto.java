package com.suprsyncr.seller.dto;

import com.suprsyncr.seller.entity.SellerStatus;

import java.time.LocalDateTime;

/**
 * DTO for seller profile information.
 */
public record SellerDto(
    Long id,
    String businessName,
    String gstin,
    String businessAddress,
    String phoneNumber,
    SellerStatus status,
    LocalDateTime createdAt
) {}

