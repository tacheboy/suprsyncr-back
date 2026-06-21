package com.suprsyncr.product.studio.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.suprsyncr.product.studio.entity.ProductDraft;
import com.suprsyncr.product.studio.entity.ProductDraftStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The shape the frontend renders for one Product Studio draft. Includes the
 * three columns as raw JsonNode so the UI can edit fields inline before
 * publishing without the backend imposing a fixed schema.
 */
@Data
@Builder
public class DraftDto {

    private UUID draftId;
    private String storeId;
    private ProductDraftStatus status;

    private String imageUrl;
    private String claimedTitle;

    private JsonNode copyColumn;
    private JsonNode seoColumn;
    private JsonNode metadataColumn;

    private JsonNode identifiedProduct;
    private JsonNode mismatchWarning;

    private String modelPath;
    private BigDecimal costInr;
    private BigDecimal confidence;
    private String planReasoning;

    private Long publishedProductId;
    private String shopifyProductId;

    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime aiCompletedAt;
    private LocalDateTime publishedAt;

    public static DraftDto from(ProductDraft d, com.fasterxml.jackson.databind.ObjectMapper om) {
        return DraftDto.builder()
                .draftId(d.getDraftId())
                .storeId(d.getStoreId())
                .status(d.getStatus())
                .imageUrl(d.getImageUrl())
                .claimedTitle(d.getClaimedTitle())
                .copyColumn(parse(om, d.getCopyColumn()))
                .seoColumn(parse(om, d.getSeoColumn()))
                .metadataColumn(parse(om, d.getMetadataColumn()))
                .identifiedProduct(parse(om, d.getIdentifiedProduct()))
                .mismatchWarning(parse(om, d.getMismatchWarning()))
                .modelPath(d.getModelPath())
                .costInr(d.getCostInr())
                .confidence(d.getConfidence())
                .planReasoning(d.getPlanReasoning())
                .publishedProductId(d.getPublishedProductId())
                .shopifyProductId(d.getShopifyProductId())
                .errorMessage(d.getErrorMessage())
                .createdAt(d.getCreatedAt())
                .aiCompletedAt(d.getAiCompletedAt())
                .publishedAt(d.getPublishedAt())
                .build();
    }

    private static JsonNode parse(com.fasterxml.jackson.databind.ObjectMapper om, String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return om.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
