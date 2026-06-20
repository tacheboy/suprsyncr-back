package com.suprsyncr.autopilot.service;

import com.suprsyncr.autopilot.domain.ChangeImpact;
import com.suprsyncr.autopilot.domain.ProposedChangeEntity;
import com.suprsyncr.autopilot.repository.ChangeImpactRepository;
import com.suprsyncr.autopilot.repository.ProposedChangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Impact Tracker â€” measures the before/after impact of applied changes.
 *
 * After a change has been applied for ~7 days, this service:
 * 1. Finds the change's pre-apply baseline metrics
 * 2. Reads the current (post-apply) metrics
 * 3. Computes the delta
 * 4. Assigns attribution confidence
 * 5. Persists to change_impact table
 *
 * Note: For MVP with dummy data, the impact computation produces reasonable
 * mock deltas. With live Shopify data (Phase 5), this will use real metrics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImpactTrackerService {

    private final ProposedChangeRepository changeRepository;
    private final ChangeImpactRepository impactRepository;

    /**
     * Find and measure impact for all changes applied ~7 days ago.
     * Called by DailyAgentRunJob scheduler.
     */
    @Transactional
    public void measurePendingImpact() {
        // Find changes applied between 6 and 8 days ago (to handle scheduling jitter)
        LocalDateTime from = LocalDateTime.now().minusDays(8);
        LocalDateTime to = LocalDateTime.now().minusDays(6);

        List<ProposedChangeEntity> candidates = changeRepository.findAppliedBetween(from, to);

        if (candidates.isEmpty()) {
            log.info("No changes found needing impact measurement");
            return;
        }

        log.info("Measuring impact for {} changes applied ~7 days ago", candidates.size());

        for (ProposedChangeEntity change : candidates) {
            try {
                measureImpact(change);
            } catch (Exception e) {
                log.error("Failed to measure impact for change {}: {}", change.getChangeId(), e.getMessage());
            }
        }
    }

    /**
     * Measure impact for a single applied change.
     */
    @Transactional
    public void measureImpact(ProposedChangeEntity change) {
        // Check if already measured
        List<ChangeImpact> existing = impactRepository.findByChangeId(change.getChangeId());
        if (!existing.isEmpty()) {
            log.debug("Impact already measured for change {}", change.getChangeId());
            return;
        }

        LocalDate appliedDate = change.getAppliedAt().toLocalDate();
        LocalDate baselineStart = appliedDate.minusDays(7);
        LocalDate baselineEnd = appliedDate.minusDays(1);
        LocalDate measurementStart = appliedDate.plusDays(1);
        LocalDate measurementEnd = appliedDate.plusDays(7);

        // Determine metric type based on change type
        String metricType = getMetricType(change.getChangeType());

        /*
         * For MVP: compute a simulated delta based on the change type.
         * In production (Phase 5), this reads real metrics from StoreDataProvider:
         *   - PRODUCT_TITLE/DESCRIPTION â†’ conversion rate from Shopify analytics
         *   - META_TITLE/DESCRIPTION â†’ organic CTR from Google Search Console
         *   - PRICE_CHANGE â†’ revenue per visitor
         *   - CART_RECOVERY â†’ cart abandonment rate
         */
        BigDecimal baselineValue = computeBaselineValue(change);
        BigDecimal measuredValue = computeMeasuredValue(change, baselineValue);

        BigDecimal deltaAbsolute = measuredValue.subtract(baselineValue);
        BigDecimal deltaPercent = baselineValue.compareTo(BigDecimal.ZERO) != 0
                ? deltaAbsolute.divide(baselineValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Attribution confidence: HIGH if this is the only change to this product
        // in the measurement window, MEDIUM if concurrent changes exist
        String attribution = computeAttributionConfidence(change);

        ChangeImpact impact = ChangeImpact.builder()
                .changeId(change.getChangeId())
                .storeId(change.getStoreId())
                .metricType(metricType)
                .baselinePeriodStart(baselineStart)
                .baselinePeriodEnd(baselineEnd)
                .baselineValue(baselineValue)
                .measurementPeriodStart(measurementStart)
                .measurementPeriodEnd(measurementEnd)
                .measuredValue(measuredValue)
                .deltaAbsolute(deltaAbsolute)
                .deltaPercent(deltaPercent)
                .attributionConfidence(attribution)
                .attributionNotes("7-day pre/post comparison")
                .estimatedRevenueImpactInr(deltaAbsolute.multiply(BigDecimal.valueOf(30))) // Monthly estimate
                .build();

        impactRepository.save(impact);
        log.info("Impact measured for change {}: {}% {} in {}",
                change.getChangeId(), deltaPercent.setScale(1, RoundingMode.HALF_UP), metricType, attribution);
    }

    /**
     * Get all impact records for a store.
     */
    public List<ChangeImpact> getImpactForStore(String storeId) {
        return impactRepository.findByStoreId(storeId);
    }

    // â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String getMetricType(String changeType) {
        return switch (changeType) {
            case "PRODUCT_TITLE", "PRODUCT_DESCRIPTION" -> "CONVERSION_RATE";
            case "META_TITLE", "META_DESCRIPTION" -> "ORGANIC_TRAFFIC";
            case "PRICE_CHANGE" -> "REVENUE";
            case "CART_RECOVERY_COPY" -> "CART_ABANDONMENT";
            default -> "CONVERSION_RATE";
        };
    }

    /**
     * Compute baseline value.
     * For MVP: extracts from the change's estimated_impact if available,
     * or uses reasonable defaults.
     */
    private BigDecimal computeBaselineValue(ProposedChangeEntity change) {
        // In production, this would query real metrics from StoreDataProvider.
        // For MVP, use reasonable defaults per metric type.
        return switch (getMetricType(change.getChangeType())) {
            case "CONVERSION_RATE" -> BigDecimal.valueOf(1.8);    // 1.8% baseline CVR
            case "ORGANIC_TRAFFIC" -> BigDecimal.valueOf(120);    // 120 clicks/week baseline
            case "REVENUE" -> BigDecimal.valueOf(15000);          // â‚¹15,000/week baseline
            case "CART_ABANDONMENT" -> BigDecimal.valueOf(72);    // 72% baseline abandonment
            default -> BigDecimal.valueOf(1.0);
        };
    }

    /**
     * Compute post-change measured value.
     * For MVP: applies a reasonable improvement range based on change type.
     */
    private BigDecimal computeMeasuredValue(ProposedChangeEntity change, BigDecimal baseline) {
        // Use change ID hash as seed for deterministic "random" improvements.
        // Same change always produces the same impact numbers across restarts.
        java.util.Random deterministicRandom = new java.util.Random(change.getChangeId().hashCode());
        double improvement = switch (change.getChangeType()) {
            case "PRODUCT_TITLE" -> 0.10 + deterministicRandom.nextDouble() * 0.15;       // 10-25% CVR lift
            case "PRODUCT_DESCRIPTION" -> 0.08 + deterministicRandom.nextDouble() * 0.12; // 8-20% CVR lift
            case "META_TITLE" -> 0.15 + deterministicRandom.nextDouble() * 0.25;          // 15-40% CTR lift
            case "META_DESCRIPTION" -> 0.10 + deterministicRandom.nextDouble() * 0.20;    // 10-30% CTR lift
            case "PRICE_CHANGE" -> -0.05 + deterministicRandom.nextDouble() * 0.15;       // -5% to +10% revenue
            case "CART_RECOVERY_COPY" -> -0.05 - deterministicRandom.nextDouble() * 0.10; // 5-15% abandonment drop
            default -> 0.05;
        };

        return baseline.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(improvement)));
    }

    /**
     * Determine attribution confidence.
     */
    private String computeAttributionConfidence(ProposedChangeEntity change) {
        // Check if there are concurrent changes to the same product
        LocalDateTime from = change.getAppliedAt().minusDays(1);
        LocalDateTime to = change.getAppliedAt().plusDays(1);
        List<ProposedChangeEntity> concurrent = changeRepository.findAppliedBetween(from, to);

        long sameEntityChanges = concurrent.stream()
                .filter(c -> c.getShopifyEntityId().equals(change.getShopifyEntityId()))
                .filter(c -> !c.getChangeId().equals(change.getChangeId()))
                .count();

        if (sameEntityChanges == 0) return "HIGH";
        if (sameEntityChanges <= 2) return "MEDIUM";
        return "LOW";
    }
}

