package com.suprsyncr.product.controller;

import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.product.dto.CategoryDto;
import com.suprsyncr.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for category management.
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Product category management endpoints")
public class CategoryController {
    
    private final CategoryService categoryService;
    
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category", description = "Creates a new product category (Admin only)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or category name already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized (requires ADMIN role)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(
        @RequestParam String name,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Long parentId
    ) {
        CategoryDto category = categoryService.createCategory(name, description, parentId);
        ApiResponse<CategoryDto> response = new ApiResponse<>(
            true,
            category,
            "Category created successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieves all product categories (public endpoint)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAllCategories() {
        List<CategoryDto> categories = categoryService.getAllCategories();
        ApiResponse<List<CategoryDto>> response = new ApiResponse<>(
            true,
            categories,
            "Categories retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{categoryId}")
    @Operation(summary = "Get category", description = "Retrieves a single category by ID (public endpoint)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<CategoryDto>> getCategory(@PathVariable Long categoryId) {
        CategoryDto category = categoryService.getCategory(categoryId);
        ApiResponse<CategoryDto> response = new ApiResponse<>(
            true,
            category,
            "Category retrieved successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete category", description = "Deletes a category and sets child categories' parent to NULL (Admin only)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized (requires ADMIN role)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        ApiResponse<Void> response = new ApiResponse<>(
            true,
            null,
            "Category deleted successfully",
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
}

