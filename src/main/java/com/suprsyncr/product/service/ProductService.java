package com.suprsyncr.product.service;

import com.suprsyncr.common.dto.PageResponse;
import com.suprsyncr.product.dto.*;
import com.suprsyncr.product.entity.ProductStatus;

/**
 * Service interface for product management.
 */
public interface ProductService {
    
    ProductDto createProduct(CreateProductRequest request);
    
    ProductDto getProduct(Long productId);
    
    PageResponse<ProductDto> getProducts(int page, int size, String search, Long categoryId, ProductStatus status);
    
    ProductDto updateProduct(Long productId, CreateProductRequest request);
    
    void deleteProduct(Long productId);
    
    ProductDto updateStatus(Long productId, ProductStatus status);
    
    ImageUploadResponse generateUploadUrl(ImageUploadRequest request);
    
    void confirmImageUpload(Long productId, String imageKey);
}

