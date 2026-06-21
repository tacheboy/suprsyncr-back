package com.suprsyncr.product.studio.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Body for POST /api/v1/products/studio/drafts/{draftId}/publish.
 *
 * The seller may have edited the three column JSON blobs inline before clicking
 * "Save and List"; the edited versions arrive here and become the source of
 * truth for the local Product (and the Shopify push, when enabled). The three
 * column fields are optional — when omitted we use whatever the engine
 * produced.
 *
 * basePriceInr is OPTIONAL — many sellers price after listing (or let Shopify
 * draft-status handle it). When omitted the product is created at price 0 and
 * the seller sets it later. The engine never sets price.
 */
@Data
public class PublishDraftRequest {

    private JsonNode copyColumn;
    private JsonNode seoColumn;
    private JsonNode metadataColumn;

    /** Optional. Null/absent → product created at ₹0 for the seller to set later. */
    private BigDecimal basePriceInr;

    private String skuOverride;
    private Boolean acceptMismatchOverride;
}
