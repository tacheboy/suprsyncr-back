package com.suprsyncr.common.exception;

/**
 * Exception thrown when a marketplace API call fails.
 * Maps to HTTP 502 Bad Gateway response.
 */
public class MarketplaceApiException extends RuntimeException {
    
    private final String platformType;
    private final Integer statusCode;
    
    public MarketplaceApiException(String platformType, Integer statusCode, String message) {
        super(message);
        this.platformType = platformType;
        this.statusCode = statusCode;
    }
    
    public MarketplaceApiException(String platformType, Integer statusCode, String message, Throwable cause) {
        super(message, cause);
        this.platformType = platformType;
        this.statusCode = statusCode;
    }
    
    public String getPlatformType() {
        return platformType;
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
}

