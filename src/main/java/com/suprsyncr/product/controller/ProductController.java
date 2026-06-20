package com.suprsyncr.product.controller;

import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.common.dto.PageResponse;
import com.suprsyncr.product.dto.*;
import com.suprsyncr.product.entity.ProductStatus;
import com.suprsyncr.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST controller for product management.
 */
@RestController
@RequestMapping("/api/v1/products")
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Products", description = "Product catalog management endpoints including variants and images")
public class ProductController {
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new product with variants in the seller's catalog")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or SKU already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
        @Valid @RequestBody CreateProductRequest request
    ) {
        ProductDto product = productService.createProduct(request);
        ApiResponse<ProductDto> response = new ApiResponse<>(
            true,
            product,
            "Product created successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{productId}")
    @Operation(summary = "Get product", description = "Retrieves a single product by ID with all variants")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable Long productId) {
        ProductDto product = productService.getProduct(productId);
        ApiResponse<ProductDto> response = new ApiResponse<>(
            true,
            product,
            "Product retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Get products", description = "Retrieves paginated products with optional filters for search, category, and status")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PageResponse<ProductDto>>> getProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) ProductStatus status
    ) {
        PageResponse<ProductDto> products = productService.getProducts(page, size, search, categoryId, status);
        ApiResponse<PageResponse<ProductDto>> response = new ApiResponse<>(
            true,
            products,
            "Products retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{productId}")
    @Operation(summary = "Update product", description = "Updates an existing product and its variants")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
        @PathVariable Long productId,
        @Valid @RequestBody CreateProductRequest request
    ) {
        ProductDto product = productService.updateProduct(productId, request);
        ApiResponse<ProductDto> response = new ApiResponse<>(
            true,
            product,
            "Product updated successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete product", description = "Deletes a product if no active listings exist")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cannot delete product with active listings"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        ApiResponse<Void> response = new ApiResponse<>(
            true,
            null,
            "Product deleted successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{productId}/status")
    @Operation(summary = "Update product status", description = "Changes the product status (DRAFT, ACTIVE, ARCHIVED)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<ProductDto>> updateStatus(
        @PathVariable Long productId,
        @RequestParam ProductStatus status
    ) {
        ProductDto product = productService.updateStatus(productId, status);
        ApiResponse<ProductDto> response = new ApiResponse<>(
            true,
            product,
            "Product status updated successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/images/upload-url")
    @Operation(summary = "Generate image upload URL", description = "Generates a pre-signed S3 URL for uploading product images")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Upload URL generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid content type"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<ImageUploadResponse>> generateUploadUrl(
        @Valid @RequestBody ImageUploadRequest request
    ) {
        ImageUploadResponse uploadResponse = productService.generateUploadUrl(request);
        ApiResponse<ImageUploadResponse> response = new ApiResponse<>(
            true,
            uploadResponse,
            "Upload URL generated successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{productId}/images/confirm")
    @Operation(summary = "Confirm image upload", description = "Associates an uploaded image with a product")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image upload confirmed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Void>> confirmImageUpload(
        @PathVariable Long productId,
        @RequestParam String imageKey
    ) {
        productService.confirmImageUpload(productId, imageKey);
        ApiResponse<Void> response = new ApiResponse<>(
            true,
            null,
            "Image upload confirmed successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
}

