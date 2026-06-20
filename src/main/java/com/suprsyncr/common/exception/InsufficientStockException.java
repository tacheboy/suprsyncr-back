package com.suprsyncr.common.exception;

/**
 * Exception thrown when attempting to reserve more inventory than available.
 * Maps to HTTP 409 Conflict response.
 */
public class InsufficientStockException extends RuntimeException {
    
    private final Long productVariantId;
    private final Integer requested;
    private final Integer available;
    
    public InsufficientStockException(Long productVariantId, Integer requested, Integer available) {
        super(String.format("Insufficient stock for product variant %d: requested %d, available %d", 
            productVariantId, requested, available));
        this.productVariantId = productVariantId;
        this.requested = requested;
        this.available = available;
    }
    
    public InsufficientStockException(Long productVariantId, Integer requested, Integer available, String message) {
        super(message);
        this.productVariantId = productVariantId;
        this.requested = requested;
        this.available = available;
    }
    
    public Long getProductVariantId() {
        return productVariantId;
    }
    
    public Integer getRequested() {
        return requested;
    }
    
    public Integer getAvailable() {
        return available;
    }
}

