package com.suprsyncr.autopilot.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the entity_snapshots table.
 * Stores the pre-change state of a Shopify entity for rollback reconstruction.
 */
@Entity
@Table(name = "entity_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EntitySnapshot {

    @Id
    @Column(name = "snapshot_id")
    private UUID snapshotId;

    @Column(name = "change_id")
    private UUID changeId;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_data")
    private String snapshotData;

    @Column(name = "snapshotted_at")
    private LocalDateTime snapshottedAt;

    @PrePersist
    public void prePersist() {
        if (snapshotId == null) snapshotId = UUID.randomUUID();
        if (snapshottedAt == null) snapshottedAt = LocalDateTime.now();
    }
}

