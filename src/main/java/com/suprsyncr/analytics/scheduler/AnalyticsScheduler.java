package com.suprsyncr.analytics.scheduler;

import com.suprsyncr.analytics.repository.DummyStoreRepository;
import com.suprsyncr.analytics.service.AnalyticsOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Phase 4 â€” Daily analytics recompute scheduler.
 *
 * Runs every day at 02:00 (IST) to ensure the frontend always reads
 * from fresh pre-computed snapshots, never triggering heavy computation
 * at page load time.
 *
 * The scheduler iterates over ALL known dummy stores (in Phase 5
 * this will iterate over all connected live stores).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsScheduler {

    private final AnalyticsOrchestrator orchestrator;
    private final DummyStoreRepository storeRepository;

    /**
     * Daily recompute â€” 02:00 AM every day.
     * Cron: "0 0 2 * * *" = second=0, minute=0, hour=2, every day.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void dailyRecompute() {
        log.info("=== Analytics daily recompute started ===");

        storeRepository.findAll().forEach(store -> {
            try {
                log.info("Recomputing analytics for store: {} ({})", store.getStoreId(), store.getStoreName());
                orchestrator.recomputeAll(store.getStoreId());
            } catch (Exception e) {
                log.error("Failed to recompute analytics for store {}: {}", store.getStoreId(), e.getMessage(), e);
                // Continue with other stores â€” one failure should not block the rest
            }
        });

        log.info("=== Analytics daily recompute complete ===");
    }
}

