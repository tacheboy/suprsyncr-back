package com.suprsyncr.product.dto;

/**
 * DTO for image upload URL response.
 */
public record ImageUploadResponse(
    String uploadUrl,
    String imageUrl,
    String key
) {}

