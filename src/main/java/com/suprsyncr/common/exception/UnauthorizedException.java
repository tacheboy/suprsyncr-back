package com.suprsyncr.common.exception;

/**
 * Exception thrown when authentication fails or credentials are invalid.
 * Maps to HTTP 401 Unauthorized response.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}

