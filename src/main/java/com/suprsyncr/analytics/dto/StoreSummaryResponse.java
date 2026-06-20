package com.suprsyncr.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;

/**
 * Top-level store summary response for the dummy API.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoreSummaryResponse {

    private String storeId;
    private String storeName;
    private String category;
    private String dataSource;

    private Integer monthlyTraffic;
    private BigDecimal avgOrderValue;
    private String primaryTrafficSource;

    // Aggregate funnel metrics
    private Integer totalProducts;
    private BigDecimal totalMonthlyRevenue;
    private Double overallConversionRate;
    private Double overallAbandonmentRate;
    private Integer totalMonthlyOrders;
}

