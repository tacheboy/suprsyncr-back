package com.suprsyncr.autopilot.attribution.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.autopilot.attribution.Attribution;
import com.suprsyncr.autopilot.attribution.AttributionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Frontend-facing shape for the Impact Lab attribution panel. JSON columns
 * are exposed as JsonNode so the UI can render structure without a fixed
 * client-side schema.
 */
@Data
@Builder
public class AttributionDto {
    private UUID attributionId;
    private Long orderId;
    private String storeId;
    private Long productId;
    private AttributionStatus status;

    private UUID causalChangeId;
    private String causalChangeType;
    private BigDecimal confidence;
    private String reasoning;
    private JsonNode pattern;

    private BigDecimal orderRevenueInr;
    private BigDecimal realizedRevenueInr;
    private BigDecimal forecastedLiftInr;

    private Integer proposalsGenerated;
    private JsonNode generatedChangeIds;

    private BigDecimal totalCostInr;
    private String modelPath;
    private String planReasoning;
    private JsonNode verifier;

    private String errorMessage;
    private LocalDateTime triggeredAt;
    private LocalDateTime completedAt;

    public static AttributionDto from(Attribution a, ObjectMapper om) {
        return AttributionDto.builder()
                .attributionId(a.getAttributionId())
                .orderId(a.getOrderId())
                .storeId(a.getStoreId())
                .productId(a.getProductId())
                .status(a.getStatus())
                .causalChangeId(a.getCausalChangeId())
                .causalChangeType(a.getCausalChangeType())
                .confidence(a.getConfidence())
                .reasoning(a.getReasoning())
                .pattern(parse(om, a.getPattern()))
                .orderRevenueInr(a.getOrderRevenueInr())
                .realizedRevenueInr(a.getRealizedRevenueInr())
                .forecastedLiftInr(a.getForecastedLiftInr())
                .proposalsGenerated(a.getProposalsGenerated())
                .generatedChangeIds(parse(om, a.getGeneratedChangeIds()))
                .totalCostInr(a.getTotalCostInr())
                .modelPath(a.getModelPath())
                .planReasoning(a.getPlanReasoning())
                .verifier(parse(om, a.getVerifier()))
                .errorMessage(a.getErrorMessage())
                .triggeredAt(a.getTriggeredAt())
                .completedAt(a.getCompletedAt())
                .build();
    }

    private static JsonNode parse(ObjectMapper om, String json) {
        if (json == null || json.isBlank()) return null;
        try { return om.readTree(json); } catch (Exception e) { return null; }
    }
}
