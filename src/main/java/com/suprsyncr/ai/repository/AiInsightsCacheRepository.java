package com.suprsyncr.ai.repository;

import com.suprsyncr.ai.domain.AiInsightsCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiInsightsCacheRepository extends JpaRepository<AiInsightsCache, UUID> {
    Optional<AiInsightsCache> findBySellerIdAndInsightTypeAndPeriodStart(Long sellerId, String insightType, LocalDate periodStart);
}

