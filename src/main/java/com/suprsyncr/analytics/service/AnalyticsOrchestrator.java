package com.suprsyncr.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.analytics.domain.AnalyticsSnapshot;
import com.suprsyncr.analytics.domain.AnalyticsSnapshot.SnapshotType;
import com.suprsyncr.analytics.dto.ProductHealthResponse;
import com.suprsyncr.analytics.dto.RevenueLeakResponse;
import com.suprsyncr.analytics.dto.SeoGapResponse;
import com.suprsyncr.analytics.provider.StoreDataProvider;
import com.suprsyncr.analytics.repository.AnalyticsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Orchestration service that coordinates compute â†’ enrich with AI â†’ persist.
 *
 * Read path: always serves from latest AnalyticsSnapshot.
 * Write path: scheduled job triggers recompute â†’ calls GeminiInsightService â†’ persists.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsOrchestrator {

    private final RevenueLeakService revenueLeakService;
    private final ProductHealthService productHealthService;
    private final SeoGapService seoGapService;
    private final GeminiInsightService geminiInsightService;
    private final AnalyticsSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    private final StoreDataProvider dataProvider;

    // â”€â”€â”€ READ PATHS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional(readOnly = true)
    public RevenueLeakResponse getRevenueLeak(String storeId) {
        return getFromSnapshot(storeId, SnapshotType.REVENUE_LEAK, RevenueLeakResponse.class)
                .orElseGet(() -> revenueLeakService.compute(storeId));
    }

    @Transactional(readOnly = true)
    public ProductHealthResponse getProductHealth(String storeId) {
        return getFromSnapshot(storeId, SnapshotType.PRODUCT_HEALTH, ProductHealthResponse.class)
                .orElseGet(() -> productHealthService.compute(storeId));
    }

    @Transactional(readOnly = true)
    public SeoGapResponse getSeoGaps(String storeId) {
        return getFromSnapshot(storeId, SnapshotType.SEO_GAP, SeoGapResponse.class)
                .orElseGet(() -> seoGapService.compute(storeId));
    }

    // â”€â”€â”€ WRITE PATHS (called by scheduler / force-refresh) â”€â”€â”€â”€

    @Transactional
    public void recomputeAll(String storeId) {
        log.info("Recomputing all analytics for store: {}", storeId);
        recomputeRevenueLeak(storeId);
        recomputeProductHealth(storeId);
        recomputeSeoGaps(storeId);
        log.info("Analytics recompute complete for store: {}", storeId);
    }

    @Transactional
    public RevenueLeakResponse recomputeRevenueLeak(String storeId) {
        RevenueLeakResponse result = revenueLeakService.compute(storeId);

        // Enrich with Gemini insight (Phase 4)
        try {
            String note = geminiInsightService.generateRevenuLeakInsight(result);
            result.setAnalystNote(note);
        } catch (Exception e) {
            log.warn("Gemini insight skipped for revenue leak: {}", e.getMessage());
        }

        persist(storeId, SnapshotType.REVENUE_LEAK, result);
        return result;
    }

    @Transactional
    public ProductHealthResponse recomputeProductHealth(String storeId) {
        ProductHealthResponse result = productHealthService.compute(storeId);

        try {
            String note = geminiInsightService.generateProductHealthInsight(result);
            result.setAnalystNote(note);
        } catch (Exception e) {
            log.warn("Gemini insight skipped for product health: {}", e.getMessage());
        }

        persist(storeId, SnapshotType.PRODUCT_HEALTH, result);
        return result;
    }

    @Transactional
    public SeoGapResponse recomputeSeoGaps(String storeId) {
        SeoGapResponse result = seoGapService.compute(storeId);

        try {
            String note = geminiInsightService.generateSeoGapInsight(result);
            result.setAnalystNote(note);
        } catch (Exception e) {
            log.warn("Gemini insight skipped for SEO gap: {}", e.getMessage());
        }

        persist(storeId, SnapshotType.SEO_GAP, result);
        return result;
    }

    // â”€â”€â”€ HELPERS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private <T> Optional<T> getFromSnapshot(String storeId, SnapshotType type, Class<T> clazz) {
        return snapshotRepository.findLatest(storeId, type)
                .flatMap(snapshot -> {
                    try {
                        return Optional.of(objectMapper.readValue(snapshot.getPayload(), clazz));
                    } catch (Exception e) {
                        log.warn("Failed to deserialize snapshot [{}/{}]: {}", storeId, type, e.getMessage());
                        return Optional.empty();
                    }
                });
    }

    private void persist(String storeId, SnapshotType type, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            // Delete today's existing snapshot for this type if any
            snapshotRepository.findLatest(storeId, type)
                    .filter(s -> s.getComputedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                    .ifPresent(snapshotRepository::delete);

            AnalyticsSnapshot snapshot = AnalyticsSnapshot.builder()
                    .storeId(storeId)
                    .type(type)
                    .computedAt(LocalDateTime.now())
                    .payload(json)
                    .dataSource(dataProvider.getDataSource())
                    .build();
            snapshotRepository.save(snapshot);
        } catch (Exception e) {
            log.error("Failed to persist analytics snapshot [{}/{}]: {}", storeId, type, e.getMessage());
        }
    }
}

