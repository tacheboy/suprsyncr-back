package com.suprsyncr.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product Health Matrix response â€” Phase 2.
 * Classifies every product into one of 4 quadrants:
 *   WINNER | LISTING_PROBLEM | SEO_PROBLEM | WRONG_PLATFORM
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductHealthResponse {

    private String storeId;
    private String storeName;
    private String dataSource;

    /** All products with quadrant classification */
    private List<ProductQuadrant> products;

    /** Top 5 prioritised actions sorted by estimated revenue impact */
    private List<ActionQueueItem> actionQueue;

    /** AI-generated narrative (Phase 4) */
    private String analystNote;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProductQuadrant {
        private String productId;
        private String name;
        private String category;
        private BigDecimal price;

        // Raw metrics
        private Integer pageViews;
        private Double conversionRate;
        private Double abandonmentRate;

        // Percentile scores (0â€“1) within this store's product set
        private Double trafficPercentile;
        private Double conversionPercentile;

        /**
         * Quadrant classification:
         *   WINNER          â€” high traffic, high conversion
         *   LISTING_PROBLEM â€” high traffic, low conversion â†’ fix listing (Service 1)
         *   SEO_PROBLEM     â€” low traffic, high conversion â†’ improve SEO (Service 1)
         *   WRONG_PLATFORM  â€” low traffic, low conversion â†’ try other platforms (Service 3)
         */
        private String quadrant;

        /** Plain-English reason for the classification */
        private String reason;

        /** CTA label for the frontend action button */
        private String ctaLabel;

        /** Route/action the CTA should trigger */
        private String ctaAction;

        /** Estimated revenue impact per day if resolved (used for action queue ranking) */
        private BigDecimal estimatedDailyImpactINR;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ActionQueueItem {
        private Integer rank;
        private String productId;
        private String productName;
        private String action;
        private String description;
        private BigDecimal estimatedDailyImpactINR;
        private String ctaLabel;
        private String ctaAction;
    }
}

