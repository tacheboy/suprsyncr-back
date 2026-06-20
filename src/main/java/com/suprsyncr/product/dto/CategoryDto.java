package com.suprsyncr.product.dto;

/**
 * DTO for product category response.
 */
public record CategoryDto(
    Long id,
    String name,
    String description,
    Long parentId
) {}

