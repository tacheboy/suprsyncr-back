package com.suprsyncr.autopilot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for change impact data â€” used in the Impact Lab UI.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImpactDto {
    private UUID impactId;
    private UUID changeId;
    private String storeId;
    private String metricType;

    private LocalDate baselinePeriodStart;
    private LocalDate baselinePeriodEnd;
    private BigDecimal baselineValue;

    private LocalDate measurementPeriodStart;
    private LocalDate measurementPeriodEnd;
    private BigDecimal measuredValue;

    private BigDecimal deltaAbsolute;
    private BigDecimal deltaPercent;
    private String attributionConfidence;
    private String attributionNotes;
    private BigDecimal estimatedRevenueImpactInr;

    // Associated change info
    private String changeType;
    private String agentType;
    private String productName;
}

