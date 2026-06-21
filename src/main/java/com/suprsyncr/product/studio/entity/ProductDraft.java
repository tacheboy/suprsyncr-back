package com.suprsyncr.product.studio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A seller's in-progress Product Studio listing.
 *
 * Status machine: PENDING_AI → AI_COMPLETE → PUBLISHED (or FAILED on engine error).
 * The three JSON columns hold the engine's three Studio columns verbatim; the
 * controller serves them back to the UI and accepts inline edits before publish.
 * On PUBLISHED a real Product is created and the linkage is captured here.
 */
@Entity
@Table(name = "product_studio_drafts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductDraft {

    @Id
    @Column(name = "draft_id")
    private UUID draftId;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProductDraftStatus status;

    // --- input ----
    @Column(name = "image_url", nullable = false, columnDefinition = "text")
    private String imageUrl;

    @Column(name = "claimed_title", nullable = false, length = 512)
    private String claimedTitle;

    @Column(name = "posture", length = 32)
    @Builder.Default
    private String posture = "balanced";

    // --- 3-column output ----
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "copy_column")
    private String copyColumn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "seo_column")
    private String seoColumn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_column")
    private String metadataColumn;

    // --- vision identification + mismatch ----
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "identified_product")
    private String identifiedProduct;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mismatch_warning")
    private String mismatchWarning;

    // --- provenance ----
    @Column(name = "model_path", columnDefinition = "text")
    private String modelPath;

    @Column(name = "cost_inr", precision = 12, scale = 6)
    private BigDecimal costInr;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "plan_reasoning", columnDefinition = "text")
    private String planReasoning;

    // --- publish linkage ----
    @Column(name = "published_product_id")
    private Long publishedProductId;

    @Column(name = "shopify_product_id", length = 64)
    private String shopifyProductId;

    // --- diagnostics ----
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ai_completed_at")
    private LocalDateTime aiCompletedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (draftId == null) draftId = UUID.randomUUID();
        if (status == null) status = ProductDraftStatus.PENDING_AI;
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
