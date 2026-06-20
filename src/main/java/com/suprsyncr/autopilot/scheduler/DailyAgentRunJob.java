package com.suprsyncr.autopilot.scheduler;

import com.suprsyncr.analytics.repository.DummyStoreRepository;
import com.suprsyncr.autopilot.service.AgentRunOrchestratorService;
import com.suprsyncr.autopilot.service.ImpactTrackerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily scheduler for the autopilot pipeline.
 *
 * Runs at 02:30 AM (after AnalyticsScheduler at 02:00 AM) to ensure
 * the agent graph receives fresh analytics snapshots.
 *
 * Also runs impact measurement for changes applied ~7 days ago.
 *
 * Can be disabled via autopilot.daily-run-enabled=false.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "autopilot.daily-run-enabled", havingValue = "true", matchIfMissing = false)
public class DailyAgentRunJob {

    private final AgentRunOrchestratorService runOrchestrator;
    private final ImpactTrackerService impactTracker;
    private final DummyStoreRepository storeRepository;

    /**
     * Daily agent run â€” 02:30 AM.
     * Triggers a full agent pipeline for every connected store.
     */
    @Scheduled(cron = "0 30 2 * * *")
    public void dailyAgentRun() {
        log.info("=== Daily agent run started ===");

        storeRepository.findAll().forEach(store -> {
            try {
                log.info("Triggering agent run for store: {} ({})",
                        store.getStoreId(), store.getStoreName());
                runOrchestrator.executeRunAsync(runOrchestrator.startRun(store.getStoreId(), "SCHEDULED"));
            } catch (Exception e) {
                log.error("Failed to trigger agent run for store {}: {}",
                        store.getStoreId(), e.getMessage(), e);
            }
        });

        log.info("=== Daily agent run trigger complete ===");
    }

    /**
     * Impact measurement â€” 06:00 AM.
     * Measures impact for changes applied ~7 days ago.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void dailyImpactMeasurement() {
        log.info("=== Impact measurement started ===");
        try {
            impactTracker.measurePendingImpact();
        } catch (Exception e) {
            log.error("Impact measurement failed: {}", e.getMessage(), e);
        }
        log.info("=== Impact measurement complete ===");
    }
}

