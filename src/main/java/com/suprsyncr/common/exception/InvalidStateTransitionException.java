package com.suprsyncr.common.exception;

/**
 * Exception thrown when an invalid state transition is attempted.
 * Maps to HTTP 409 Conflict response.
 */
public class InvalidStateTransitionException extends RuntimeException {
    
    public InvalidStateTransitionException(String message) {
        super(message);
    }
    
    public InvalidStateTransitionException(String fromState, String toState) {
        super(String.format("Invalid state transition from %s to %s", fromState, toState));
    }
    
    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}

