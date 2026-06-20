package com.suprsyncr.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Revenue Leak response â€” the flagship Phase 1 card.
 * Shows the total revenue lost to cart abandonment + checkout drop,
 * broken down by product and traffic source.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevenueLeakResponse {

    private String storeId;
    private String storeName;
    private String dataSource;

    /** Total rupee leak across all products and sources */
    private BigDecimal totalLeakINR;

    /** Portion attributable to cart abandonment (added to cart, never bought) */
    private BigDecimal cartAbandonmentLossINR;

    /** Portion attributable to checkout drop (started checkout, never bought) */
    private BigDecimal checkoutDropLossINR;

    /** Overall cart abandonment rate (0â€“1) */
    private Double overallAbandonmentRate;

    /** Per-product breakdown sorted by leak descending */
    private List<ProductLeakBreakdown> byProduct;

    /** Per-traffic-source breakdown sorted by leak descending */
    private List<SourceLeakBreakdown> bySource;

    /** AI-generated narrative (Phase 4) â€” null until Gemini layer runs */
    private String analystNote;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProductLeakBreakdown {
        private String productId;
        private String name;
        private BigDecimal leakINR;
        private Double abandonmentRate;
        private Integer addToCartCount;
        private Integer purchaseCount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SourceLeakBreakdown {
        private String source;
        private BigDecimal leakINR;
        private Double abandonmentRate;
        private Integer sessions;
        private Integer purchasedSessions;
    }
}

