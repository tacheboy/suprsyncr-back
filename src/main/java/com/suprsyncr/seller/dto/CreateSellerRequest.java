package com.suprsyncr.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for creating a seller profile.
 */
public record CreateSellerRequest(
    @NotBlank(message = "Business name is required")
    String businessName,
    
    @NotBlank(message = "GSTIN is required")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", 
             message = "Invalid GSTIN format")
    String gstin,
    
    @NotBlank(message = "Business address is required")
    String businessAddress,
    
    @NotBlank(message = "Phone number is required")
    String phoneNumber
) {}

