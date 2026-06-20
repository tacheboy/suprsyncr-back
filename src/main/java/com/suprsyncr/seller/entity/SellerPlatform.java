package com.suprsyncr.seller.entity;

import com.suprsyncr.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing a seller's connection to a marketplace platform.
 */
@Entity
@Table(name = "seller_platforms")
public class SellerPlatform extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlatformType platformType;
    
    @Column(nullable = false)
    private String storeName;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedCredentials;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ConnectionStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AccountCreationMethod creationMethod;
    
    @Column
    private String externalStoreId;
    
    @Column
    private String platformMetadata;
    
    private LocalDateTime lastSyncedAt;
    
    @Column(columnDefinition = "TEXT")
    private String lastSyncError;
    
    // Getters and Setters
    
    public Seller getSeller() {
        return seller;
    }
    
    public void setSeller(Seller seller) {
        this.seller = seller;
    }
    
    public PlatformType getPlatformType() {
        return platformType;
    }
    
    public void setPlatformType(PlatformType platformType) {
        this.platformType = platformType;
    }
    
    public String getStoreName() {
        return storeName;
    }
    
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
    
    public String getEncryptedCredentials() {
        return encryptedCredentials;
    }
    
    public void setEncryptedCredentials(String encryptedCredentials) {
        this.encryptedCredentials = encryptedCredentials;
    }
    
    public ConnectionStatus getStatus() {
        return status;
    }
    
    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }
    
    public AccountCreationMethod getCreationMethod() {
        return creationMethod;
    }
    
    public void setCreationMethod(AccountCreationMethod creationMethod) {
        this.creationMethod = creationMethod;
    }
    
    public String getExternalStoreId() {
        return externalStoreId;
    }
    
    public void setExternalStoreId(String externalStoreId) {
        this.externalStoreId = externalStoreId;
    }
    
    public String getPlatformMetadata() {
        return platformMetadata;
    }
    
    public void setPlatformMetadata(String platformMetadata) {
        this.platformMetadata = platformMetadata;
    }
    
    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }
    
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
    
    public String getLastSyncError() {
        return lastSyncError;
    }
    
    public void setLastSyncError(String lastSyncError) {
        this.lastSyncError = lastSyncError;
    }
}

