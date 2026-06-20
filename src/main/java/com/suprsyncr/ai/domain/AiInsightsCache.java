package com.suprsyncr.ai.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_insights_cache", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"seller_id", "insight_type", "period_start"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AiInsightsCache {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(length = 20, nullable = false)
    private String insightType;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    // We keep it as String here to map to JSONB trivially 
    @Column(columnDefinition = "jsonb")
    private String content;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

