package com.suprsyncr.common.exception;

import com.suprsyncr.seller.entity.PlatformType;

/**
 * Exception thrown when a requested platform type is not supported or has no registered connector/validator.
 */
public class UnsupportedPlatformException extends RuntimeException {
    
    private final PlatformType platformType;
    
    public UnsupportedPlatformException(PlatformType platformType, String message) {
        super(message);
        this.platformType = platformType;
    }
    
    public UnsupportedPlatformException(PlatformType platformType) {
        super("Platform not supported: " + platformType);
        this.platformType = platformType;
    }
    
    public PlatformType getPlatformType() {
        return platformType;
    }
}

