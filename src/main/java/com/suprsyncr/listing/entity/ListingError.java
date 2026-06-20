package com.suprsyncr.listing.entity;

import com.suprsyncr.common.entity.BaseEntity;
import jakarta.persistence.*;

/**
 * Entity representing an error that occurred during listing publication or synchronization.
 * Provides detailed error tracking for troubleshooting marketplace integration issues.
 */
@Entity
@Table(name = "listing_errors")
public class ListingError extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(columnDefinition = "TEXT")
    private String errorDetails;
    
    @Column(nullable = false)
    private boolean resolved = false;
    
    // Getters and Setters
    
    public Listing getListing() {
        return listing;
    }
    
    public void setListing(Listing listing) {
        this.listing = listing;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorDetails() {
        return errorDetails;
    }
    
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
    
    public boolean isResolved() {
        return resolved;
    }
    
    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}

