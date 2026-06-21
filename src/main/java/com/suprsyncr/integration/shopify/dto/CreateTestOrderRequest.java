package com.suprsyncr.integration.shopify.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Creates a Shopify sandbox/test order, which exercises the real webhook path. */
public record CreateTestOrderRequest(
        @NotBlank String productTitle,
        @NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice,
        @NotNull @Min(1) Integer quantity,
        String customerEmail
) {}
