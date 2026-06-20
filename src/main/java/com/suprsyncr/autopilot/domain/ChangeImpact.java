package com.suprsyncr.autopilot.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the change_impact table.
 * Stores the measured impact of an applied change after 7 days.
 */
@Entity
@Table(name = "change_impact")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangeImpact {

    @Id
    @Column(name = "impact_id")
    private UUID impactId;

    @Column(name = "change_id")
    private UUID changeId;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Column(name = "metric_type")
    private String metricType;

    @Column(name = "baseline_period_start")
    private LocalDate baselinePeriodStart;

    @Column(name = "baseline_period_end")
    private LocalDate baselinePeriodEnd;

    @Column(name = "baseline_value")
    private BigDecimal baselineValue;

    @Column(name = "measurement_period_start")
    private LocalDate measurementPeriodStart;

    @Column(name = "measurement_period_end")
    private LocalDate measurementPeriodEnd;

    @Column(name = "measured_value")
    private BigDecimal measuredValue;

    @Column(name = "delta_absolute")
    private BigDecimal deltaAbsolute;

    @Column(name = "delta_percent")
    private BigDecimal deltaPercent;

    @Column(name = "attribution_confidence")
    private String attributionConfidence;

    @Column(name = "attribution_notes", columnDefinition = "text")
    private String attributionNotes;

    @Column(name = "estimated_revenue_impact_inr")
    private BigDecimal estimatedRevenueImpactInr;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    @PrePersist
    public void prePersist() {
        if (impactId == null) impactId = UUID.randomUUID();
        if (computedAt == null) computedAt = LocalDateTime.now();
    }
}

