package com.suprsyncr.autopilot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full DTO for proposed changes â€” used in API responses.
 * Maps from ProposedChangeEntity with all fields the frontend needs.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProposedChangeDto {
    private UUID changeId;
    private String storeId;
    private UUID runId;
    private String agentType;
    private String changeType;
    private String shopifyEntityType;
    private String shopifyEntityId;
    private String productName;

    private JsonNode currentValue;
    private JsonNode proposedValue;

    private String agentReasoning;
    private JsonNode estimatedImpact;
    private String riskLevel;
    private String riskNotes;
    private String criticVerdict;
    private String criticNotes;
    private String status;

    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime appliedAt;
    private LocalDateTime rollbackAvailableUntil;

    private Boolean isTest;
    private LocalDateTime testRevertAt;

    private java.util.List<String> humanActionItems;
}

