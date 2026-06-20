package com.suprsyncr.ai.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_requests", indexes = {
    @Index(name = "idx_ai_req_feature_date", columnList = "feature, created_at"),
    @Index(name = "idx_ai_req_seller_date", columnList = "seller_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AiRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false, length = 50)
    private String feature;

    @Column(columnDefinition = "TEXT")
    private String inputSummary;

    @Column(length = 50)
    private String geminiModel;

    private Integer promptTokens;
    private Integer outputTokens;
    private Integer latencyMs;

    @Column(length = 50)
    private String status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

