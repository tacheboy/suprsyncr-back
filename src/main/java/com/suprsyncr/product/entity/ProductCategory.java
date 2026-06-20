package com.suprsyncr.product.entity;

import com.suprsyncr.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a product category with hierarchical structure.
 */
@Entity
@Table(name = "product_categories")
public class ProductCategory extends BaseEntity {
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ProductCategory parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<ProductCategory> children = new ArrayList<>();
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ProductCategory getParent() {
        return parent;
    }
    
    public void setParent(ProductCategory parent) {
        this.parent = parent;
    }
    
    public List<ProductCategory> getChildren() {
        return children;
    }
    
    public void setChildren(List<ProductCategory> children) {
        this.children = children;
    }
}

