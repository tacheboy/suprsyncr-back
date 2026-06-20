package com.suprsyncr.product.service;

import com.suprsyncr.common.exception.ResourceNotFoundException;
import com.suprsyncr.common.exception.ValidationException;
import com.suprsyncr.product.dto.CategoryDto;
import com.suprsyncr.product.entity.ProductCategory;
import com.suprsyncr.product.repository.ProductCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of CategoryService.
 */
@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {
    
    private final ProductCategoryRepository categoryRepository;
    
    public CategoryServiceImpl(ProductCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    
    @Override
    public CategoryDto createCategory(String name, String description, Long parentId) {
        // Validate name uniqueness
        if (categoryRepository.findByName(name).isPresent()) {
            throw new ValidationException("Category with name " + name + " already exists");
        }
        
        ProductCategory category = new ProductCategory();
        category.setName(name);
        category.setDescription(description);
        
        // Set parent if provided
        if (parentId != null) {
            ProductCategory parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParent(parent);
        }
        
        category = categoryRepository.save(category);
        
        return toDto(category);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategory(Long categoryId) {
        ProductCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        return toDto(category);
    }
    
    @Override
    public void deleteCategory(Long categoryId) {
        ProductCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        // Set parent to null for all child categories
        for (ProductCategory child : category.getChildren()) {
            child.setParent(null);
        }
        
        categoryRepository.delete(category);
    }
    
    private CategoryDto toDto(ProductCategory category) {
        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getParent() != null ? category.getParent().getId() : null
        );
    }
}

