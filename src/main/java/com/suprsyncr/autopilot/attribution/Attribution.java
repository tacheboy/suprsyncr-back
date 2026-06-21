package com.suprsyncr.autopilot.attribution;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row per attribution attempt against one order.
 *
 * The {@code order_id} uniqueness is also the poller's dedup key: if a row
 * exists for an order, we never re-run the gate or the engine for it.
 * GATE_SKIPPED is recorded so non-attributable orders don't keep being
 * checked on every poll cycle.
 */
@Entity
@Table(name = "attributions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attribution {

    @Id
    @Column(name = "attribution_id")
    private UUID attributionId;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "product_id")
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AttributionStatus status;

    // --- engine verdict ----
    @Column(name = "causal_change_id")
    private UUID causalChangeId;

    @Column(name = "causal_change_type", length = 100)
    private String causalChangeType;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "reasoning", columnDefinition = "text")
    private String reasoning;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pattern")
    private String pattern;

    // --- revenue tracking ----
    @Column(name = "order_revenue_inr", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal orderRevenueInr = BigDecimal.ZERO;

    @Column(name = "realized_revenue_inr", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal realizedRevenueInr = BigDecimal.ZERO;

    @Column(name = "forecasted_lift_inr", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal forecastedLiftInr = BigDecimal.ZERO;

    // --- propagation ----
    @Column(name = "proposals_generated", nullable = false)
    @Builder.Default
    private Integer proposalsGenerated = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_change_ids")
    private String generatedChangeIds;

    // --- engine telemetry ----
    @Column(name = "total_cost_inr", precision = 12, scale = 6)
    private BigDecimal totalCostInr;

    @Column(name = "model_path", columnDefinition = "text")
    private String modelPath;

    @Column(name = "plan_reasoning", columnDefinition = "text")
    private String planReasoning;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "invocations")
    private String invocations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "verifier")
    private String verifier;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (attributionId == null) attributionId = UUID.randomUUID();
        if (triggeredAt == null) triggeredAt = LocalDateTime.now();
        if (status == null) status = AttributionStatus.PENDING;
    }
}
