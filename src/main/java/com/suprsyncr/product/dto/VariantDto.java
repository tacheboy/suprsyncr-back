package com.suprsyncr.product.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for product variant response.
 */
public record VariantDto(
    Long id,
    String sku,
    String variantName,
    Map<String, String> attributes,
    BigDecimal price,
    String imageUrl
) {}

