package com.suprsyncr.autopilot.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the proposed_changes table.
 * Each record is a single proposed change from an agent â€” the atomic unit of the proposal system.
 *
 * Status state machine:
 *   PENDING_APPROVAL â†’ APPROVED â†’ APPLYING â†’ APPLIED
 *                    â†’ REJECTED
 *                                           â†’ APPLY_FAILED
 *                                  APPLIED  â†’ ROLLED_BACK
 */
@Entity
@Table(name = "proposed_changes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProposedChangeEntity {

    @Id
    @Column(name = "change_id")
    private UUID changeId;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "agent_type")
    private String agentType;

    @Column(name = "change_type")
    private String changeType;

    @Column(name = "shopify_entity_type")
    private String shopifyEntityType;

    @Column(name = "shopify_entity_id")
    private String shopifyEntityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_value", nullable = false)
    private String currentValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_value", nullable = false)
    private String proposedValue;

    @Column(name = "agent_reasoning", columnDefinition = "text")
    private String agentReasoning;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "estimated_impact")
    private String estimatedImpact;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "risk_notes", columnDefinition = "text")
    private String riskNotes;

    @Column(name = "status")
    private String status;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "rollback_available_until")
    private LocalDateTime rollbackAvailableUntil;

    @Column(name = "is_test")
    @Builder.Default
    private Boolean isTest = false;

    @Column(name = "test_revert_at")
    private LocalDateTime testRevertAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "test_revert_condition")
    private String testRevertCondition;

    @PrePersist
    public void prePersist() {
        if (changeId == null) changeId = UUID.randomUUID();
        if (status == null) status = "PENDING_APPROVAL";
    }
}

