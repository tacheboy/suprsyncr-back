package com.suprsyncr.product.dto;

import com.suprsyncr.product.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for product response.
 */
public record ProductDto(
    Long id,
    String name,
    String description,
    CategoryDto category,
    String sku,
    BigDecimal basePrice,
    ProductStatus status,
    List<String> imageUrls,
    String brand,
    BigDecimal weight,
    BigDecimal length,
    BigDecimal width,
    BigDecimal height,
    List<VariantDto> variants,
    LocalDateTime createdAt
) {}

