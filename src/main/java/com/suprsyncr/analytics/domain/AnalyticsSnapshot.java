package com.suprsyncr.analytics.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

/**
 * Persisted analytics snapshot â€” one per store per type per day.
 * Services compute metrics on schedule; frontend always reads from here.
 * Never compute on read.
 */
@Entity
@Table(name = "analytics_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyticsSnapshot {

    public enum SnapshotType {
        REVENUE_LEAK, PRODUCT_HEALTH, SEO_GAP, FULL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private String storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SnapshotType type;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    /** JSONB payload â€” contains computed metrics + optional Gemini narrative */
    @Type(JsonBinaryType.class)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "data_source", nullable = false)
    @Builder.Default
    private String dataSource = "dummy";
}

