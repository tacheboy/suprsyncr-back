package com.suprsyncr.product.entity;

import com.suprsyncr.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/**
 * Entity representing a product variant (e.g., size, color combination).
 */
@Entity
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(nullable = false, length = 100)
    private String sku;
    
    @Column(nullable = false)
    private String variantName;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String attributes;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(length = 512)
    private String imageUrl;
    
    // Getters and Setters
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public String getVariantName() {
        return variantName;
    }
    
    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }
    
    public String getAttributes() {
        return attributes;
    }
    
    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

