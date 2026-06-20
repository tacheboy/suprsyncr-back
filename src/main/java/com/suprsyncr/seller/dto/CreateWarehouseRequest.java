package com.suprsyncr.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for creating a warehouse.
 */
public record CreateWarehouseRequest(
    @NotBlank(message = "Warehouse name is required")
    String name,
    
    @NotBlank(message = "Address is required")
    String address,
    
    @NotBlank(message = "City is required")
    String city,
    
    @NotBlank(message = "State is required")
    String state,
    
    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid pincode format")
    String pincode,
    
    boolean isDefault
) {}

