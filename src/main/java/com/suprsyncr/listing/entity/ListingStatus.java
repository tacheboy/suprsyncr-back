package com.suprsyncr.listing.entity;

/**
 * Enum representing the status of a product listing on a marketplace platform.
 */
public enum ListingStatus {
    /**
     * Listing is pending publication to the marketplace.
     */
    PENDING,
    
    /**
     * Listing has been successfully published to the marketplace.
     */
    PUBLISHED,
    
    /**
     * Listing publication failed.
     */
    FAILED,
    
    /**
     * Listing has been removed from the marketplace.
     */
    DELISTED
}

