package com.suprsyncr.product.studio.entity;

/**
 * Lifecycle of a Product Studio draft.
 *
 * PENDING_AI  → draft created, waiting on the engine
 * AI_COMPLETE → engine returned 3 columns; seller may edit and publish
 * PUBLISHED   → local Product created (and Shopify push attempted if enabled)
 * FAILED      → engine call failed or returned an unrecoverable error
 */
public enum ProductDraftStatus {
    PENDING_AI,
    AI_COMPLETE,
    PUBLISHED,
    FAILED
}
