package com.suprsyncr.autopilot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for agent run status â€” used in API responses.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentRunDto {
    private UUID runId;
    private String storeId;
    private String triggeredBy;
    private LocalDateTime triggeredAt;
    private String status;
    private String runType;
    private String selectedAgents;
    private Integer proposalsGenerated;
    private BigDecimal estimatedImpactInr;
    private LocalDateTime completedAt;
    private String errorMessage;
}

