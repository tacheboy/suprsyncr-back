package com.suprsyncr.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for creating a product variant.
 */
public record CreateVariantRequest(
    @NotBlank String sku,
    @NotBlank String variantName,
    Map<String, String> attributes,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    String imageUrl
) {}

