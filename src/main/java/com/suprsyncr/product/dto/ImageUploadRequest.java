package com.suprsyncr.product.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for requesting an image upload URL.
 */
public record ImageUploadRequest(
    @NotBlank String fileName,
    @NotBlank String contentType
) {}

