package com.suprsyncr.listing.entity;

import com.suprsyncr.common.entity.BaseEntity;
import com.suprsyncr.product.entity.Product;
import com.suprsyncr.seller.entity.SellerPlatform;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a product listing on a marketplace platform.
 * Tracks the publication status and synchronization of products to external marketplaces.
 */
@Entity
@Table(name = "listings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "platform_id"})
})
public class Listing extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    private SellerPlatform platform;
    
    @Column(nullable = false)
    private String externalProductId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ListingStatus status;
    
    private LocalDateTime publishedAt;
    
    private LocalDateTime lastSyncedAt;
    
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListingError> errors = new ArrayList<>();
    
    // Getters and Setters
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public SellerPlatform getPlatform() {
        return platform;
    }
    
    public void setPlatform(SellerPlatform platform) {
        this.platform = platform;
    }
    
    public String getExternalProductId() {
        return externalProductId;
    }
    
    public void setExternalProductId(String externalProductId) {
        this.externalProductId = externalProductId;
    }
    
    public ListingStatus getStatus() {
        return status;
    }
    
    public void setStatus(ListingStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }
    
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
    
    public List<ListingError> getErrors() {
        return errors;
    }
    
    public void setErrors(List<ListingError> errors) {
        this.errors = errors;
    }
}

