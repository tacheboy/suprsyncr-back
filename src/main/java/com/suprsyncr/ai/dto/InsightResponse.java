package com.suprsyncr.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InsightResponse {
    private String period;
    private String headline;
    private String periodStart;
    private String periodEnd;
    
    private PerformanceSummary performanceSummary;
    private List<InventoryAlert> inventoryAlerts;
    private List<PlatformInsight> platformInsights;
    private List<MarketTrend> marketTrends;
    private List<ActionItem> actionItems;
    private String nextPeriodForecast;
    private FinancialHealth financialHealth;
    private List<ProductPerformance> topPerformers;
    private List<ProductPerformance> underperformers;
    private String competitorLandscape;
    private List<StrategicRecommendation> strategicRecommendations;
    private List<NextPeriodOpportunity> nextPeriodOpportunities;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PerformanceSummary {
        private String revenueTrend;
        private String keyWin;
        private String keyConcern;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class InventoryAlert {
        private String product;
        private int unitsLeft;
        private int daysUntilStockout;
        private String action;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PlatformInsight {
        private String platform;
        private String observation;
        private String suggestedAction;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class MarketTrend {
        private String trend;
        private String relevance;
        private String opportunity;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ActionItem {
        private String priority;
        private String action;
        private String expectedImpact;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class FinancialHealth {
        private String revenueVsLastMonth;
        private String profitMarginEstimate;
        private String platformFeeObservations;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProductPerformance {
        private String product;
        private String why;
        private String diagnosis;
        private String fix;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StrategicRecommendation {
        private String recommendation;
        private String rationale;
        private String timeframe;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class NextPeriodOpportunity {
        private String opportunity;
        private String actionNeeded;
        private String deadline;
    }
    
    public static InsightResponse createPlaceholder(String headline, String observation, String periodType) {
        InsightResponse response = new InsightResponse();
        response.setPeriod(periodType);
        response.setHeadline(headline);
        ActionItem item = new ActionItem("HIGH", observation, "N/A");
        response.setActionItems(List.of(item));
        return response;
    }
}

