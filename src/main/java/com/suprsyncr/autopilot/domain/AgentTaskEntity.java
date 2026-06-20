package com.suprsyncr.autopilot.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One planned task admitted by the supervisor for a run — the unit the router
 * executes. Persisted from the engine's run telemetry.
 */
@Entity
@Table(name = "agent_tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentTaskEntity {

    @Id
    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "agent_type", nullable = false)
    private String agentType;

    @Column(name = "kind", nullable = false)
    private String kind;          // deterministic | llm

    @Column(name = "complexity")
    private String complexity;    // simple | moderate | complex

    @Column(name = "status")
    private String status;        // COMPLETE | DEGRADED | FAILED | SKIPPED

    @Column(name = "revenue_at_stake_inr")
    private BigDecimal revenueAtStakeInr;

    @Column(name = "accuracy_bar")
    private BigDecimal accuracyBar;

    @Column(name = "model_path")
    private String modelPath;

    @Column(name = "cost_inr")
    private BigDecimal costInr;

    @Column(name = "confidence")
    private BigDecimal confidence;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (taskId == null) taskId = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
