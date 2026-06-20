package com.suprsyncr.seller.entity;

/**
 * Enum representing how a platform account was created.
 */
public enum AccountCreationMethod {
    /**
     * Seller connected an existing marketplace account.
     */
    EXISTING_ACCOUNT,
    
    /**
     * Platform account was automatically created by the system.
     */
    AUTO_CREATED
}

