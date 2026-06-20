package com.suprsyncr.common.exception;

/**
 * Exception thrown when validation fails.
 * Maps to HTTP 400 Bad Request response.
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

