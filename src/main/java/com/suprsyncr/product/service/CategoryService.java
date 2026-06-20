package com.suprsyncr.product.service;

import com.suprsyncr.product.dto.CategoryDto;

import java.util.List;

/**
 * Service interface for category management.
 */
public interface CategoryService {
    
    CategoryDto createCategory(String name, String description, Long parentId);
    
    List<CategoryDto> getAllCategories();
    
    CategoryDto getCategory(Long categoryId);
    
    void deleteCategory(Long categoryId);
}

