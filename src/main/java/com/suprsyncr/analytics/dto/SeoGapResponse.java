package com.suprsyncr.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * SEO Gap Analyzer response â€” Phase 3.
 * Shows keywords almost ranking (positions 8â€“20) with estimated
 * traffic and revenue opportunity if they moved to position 3.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeoGapResponse {

    private String storeId;
    private String storeName;
    private String dataSource;

    /** Total estimated monthly revenue if all opportunities are captured */
    private BigDecimal totalOpportunityINR;

    /** Keyword opportunities sorted by revenue impact descending */
    private List<KeywordOpportunity> opportunities;

    /** AI-generated narrative (Phase 4) */
    private String analystNote;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class KeywordOpportunity {
        private String productId;
        private String productName;
        private String query;

        private Double currentPosition;
        private Double currentCtr;
        private Integer impressions;
        private Integer currentClicks;

        /** Expected CTR at target position (position 3 = 11%) */
        private Double targetCtr;
        private Double targetPosition;

        /** Additional clicks per month if ranking improves */
        private Integer estimatedNewClicksPerMonth;

        /** Estimated additional revenue per month */
        private BigDecimal estimatedRevenuePerMonth;

        /** Opportunity score = impressions Ã— (targetCTR - currentCTR) */
        private Double opportunityScore;
    }
}

