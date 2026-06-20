package com.suprsyncr.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for creating a product.
 */
public record CreateProductRequest(
    @NotBlank String name,
    String description,
    Long categoryId,
    @NotBlank String sku,
    @NotNull @DecimalMin("0.01") BigDecimal basePrice,
    String brand,
    BigDecimal weight,
    BigDecimal length,
    BigDecimal width,
    BigDecimal height,
    @Valid List<CreateVariantRequest> variants
) {}

