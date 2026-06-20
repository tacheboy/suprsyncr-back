package com.suprsyncr.seller.dto;

/**
 * DTO for warehouse information.
 */
public record WarehouseDto(
    Long id,
    String name,
    String address,
    String city,
    String state,
    String pincode,
    boolean isDefault
) {}

