package com.suprsyncr.seller.entity;

/**
 * Enum representing the connection status of a platform.
 */
public enum ConnectionStatus {
    /**
     * Platform connection is pending setup.
     */
    PENDING,
    
    /**
     * Platform account is being created.
     */
    CREATING,
    
    /**
     * Platform is successfully connected.
     */
    CONNECTED,
    
    /**
     * Platform is disconnected.
     */
    DISCONNECTED,
    
    /**
     * Platform connection has an error.
     */
    ERROR
}

