package com.suprsyncr.inventory.entity;

import com.suprsyncr.common.entity.BaseEntity;
import com.suprsyncr.product.entity.ProductVariant;
import com.suprsyncr.seller.entity.SellerWarehouse;
import jakarta.persistence.*;

/**
 * Entity representing inventory tracking for a product variant at a specific warehouse.
 * Uses optimistic locking to prevent concurrent update conflicts.
 * 
 * Requirements: 9, 10, 11, 56, 71, 82, 90, 91
 */
@Entity
@Table(name = "inventory", uniqueConstraints = {
    @UniqueConstraint(name = "uk_inventory_variant_warehouse", columnNames = {"product_variant_id", "warehouse_id"})
})
public class Inventory extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private SellerWarehouse warehouse;
    
    @Column(nullable = false)
    private Integer availableQuantity = 0;
    
    @Column(nullable = false)
    private Integer reservedQuantity = 0;
    
    @Column(nullable = false)
    private Integer totalQuantity = 0; // availableQuantity + reservedQuantity
    
    @Column(nullable = false)
    private Integer lowStockThreshold = 10;
    
    @Version
    private Long version; // Optimistic locking for concurrency control
    
    // Getters and Setters
    
    public ProductVariant getProductVariant() {
        return productVariant;
    }
    
    public void setProductVariant(ProductVariant productVariant) {
        this.productVariant = productVariant;
    }
    
    public SellerWarehouse getWarehouse() {
        return warehouse;
    }
    
    public void setWarehouse(SellerWarehouse warehouse) {
        this.warehouse = warehouse;
    }
    
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
    
    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
    
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    
    public Integer getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    
    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }
    
    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}

