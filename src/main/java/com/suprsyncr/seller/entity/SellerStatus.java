package com.suprsyncr.seller.entity;

/**
 * Enum representing the status of a seller account.
 */
public enum SellerStatus {
    /**
     * Seller profile is pending approval.
     */
    PENDING,
    
    /**
     * Seller profile is active and can use the platform.
     */
    ACTIVE,
    
    /**
     * Seller profile is suspended and cannot use the platform.
     */
    SUSPENDED
}

