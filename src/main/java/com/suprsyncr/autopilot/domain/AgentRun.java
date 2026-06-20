package com.suprsyncr.autopilot.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the agent_runs table.
 * Each record represents one full agent pipeline execution for a store.
 */
@Entity
@Table(name = "agent_runs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentRun {

    @Id
    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "triggered_by")
    private String triggeredBy;  // SCHEDULED, MANUAL, WEBHOOK

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "status")
    private String status;  // QUEUED, RUNNING, COMPLETE, FAILED

    @Column(name = "run_type")
    private String runType;  // FULL_PIPELINE, INDIVIDUAL

    @Column(name = "selected_agents")
    private String selectedAgents;  // Comma-separated agent names for individual runs

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_overrides")
    private String productOverrides;  // JSON array of product IDs

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "orchestrator_reasoning")
    private String orchestratorReasoning;

    @Column(name = "proposals_generated")
    private Integer proposalsGenerated;

    @Column(name = "estimated_impact_inr")
    private BigDecimal estimatedImpactInr;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @PrePersist
    public void prePersist() {
        if (runId == null) runId = UUID.randomUUID();
        if (triggeredAt == null) triggeredAt = LocalDateTime.now();
        if (status == null) status = "QUEUED";
        if (runType == null) runType = "FULL_PIPELINE";
    }
}

