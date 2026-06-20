package com.suprsyncr.seller.dto;

/**
 * DTO containing seller data for marketplace onboarding.
 */
public record MarketplaceOnboardingData(
    String businessName,
    String ownerName,
    String email,
    String phoneNumber,
    String gstin,
    String businessAddress,
    String city,
    String state,
    String pincode,
    String bankAccountNumber,
    String bankIfscCode,
    String bankAccountHolderName,
    WarehouseDto primaryWarehouse
) {}

