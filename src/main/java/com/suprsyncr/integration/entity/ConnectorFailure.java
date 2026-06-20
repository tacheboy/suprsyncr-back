package com.suprsyncr.integration.entity;

import com.suprsyncr.common.entity.BaseEntity;
import com.suprsyncr.seller.entity.SellerPlatform;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing a connector failure for tracking marketplace API issues.
 */
@Entity
@Table(name = "connector_failures")
public class ConnectorFailure extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    private SellerPlatform platform;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FailureType failureType;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(columnDefinition = "TEXT")
    private String stackTrace;
    
    @Column(nullable = false)
    private boolean resolved = false;
    
    private LocalDateTime resolvedAt;
    
    // Getters and Setters
    
    public SellerPlatform getPlatform() {
        return platform;
    }
    
    public void setPlatform(SellerPlatform platform) {
        this.platform = platform;
    }
    
    public FailureType getFailureType() {
        return failureType;
    }
    
    public void setFailureType(FailureType failureType) {
        this.failureType = failureType;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    public boolean isResolved() {
        return resolved;
    }
    
    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
    
    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
    
    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
