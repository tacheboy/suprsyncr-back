package com.suprsyncr.integration.connector;

import com.suprsyncr.seller.entity.PlatformType;

/**
 * Interface for validating webhook signatures from marketplace platforms.
 * Each platform implementation validates signatures using platform-specific algorithms.
 */
public interface WebhookValidator {
    
    /**
     * Validates the webhook signature.
     * 
     * @param payload the raw webhook payload
     * @param signature the signature from the webhook header
     * @param secret the platform-specific secret key for validation
     * @return true if the signature is valid, false otherwise
     */
    boolean validateSignature(String payload, String signature, String secret);
    
    /**
     * Gets the platform type this validator handles.
     * 
     * @return the platform type
     */
    PlatformType getPlatformType();
}
