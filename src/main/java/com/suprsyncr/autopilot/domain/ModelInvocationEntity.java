package com.suprsyncr.autopilot.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One model call (or a deterministic step) — the atomic cost/accuracy ledger
 * entry. Powers the ROI view and, later, router calibration.
 */
@Entity
@Table(name = "model_invocations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ModelInvocationEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "tier")
    private Short tier;

    @Column(name = "purpose")
    private String purpose;       // draft | escalate | verify | deterministic

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "cost_inr")
    private BigDecimal costInr;

    @Column(name = "verifier_passed")
    private Boolean verifierPassed;

    @Column(name = "escalated_from")
    private String escalatedFrom;

    @Column(name = "confidence")
    private BigDecimal confidence;

    @Column(name = "ok")
    private Boolean ok;

    @Column(name = "error", columnDefinition = "text")
    private String error;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
