package com.suprsyncr.analytics.service;

import com.suprsyncr.analytics.domain.DummyProduct;
import com.suprsyncr.analytics.domain.DummyProductKeyword;
import com.suprsyncr.analytics.domain.DummySessionEvent;
import com.suprsyncr.analytics.domain.DummyStore;
import com.suprsyncr.analytics.dto.SeoGapResponse;
import com.suprsyncr.analytics.dto.SeoGapResponse.KeywordOpportunity;
import com.suprsyncr.analytics.provider.StoreDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 3 â€” SEO Gap Analyzer (pure computation, no AI).
 *
 * Identifies "almost ranking" keywords (positions 8â€“20 with meaningful impressions)
 * and calculates the revenue opportunity if they moved to position 3.
 *
 * Standard CTR curve benchmarks:
 *   Position 1  â†’ 28%
 *   Position 2  â†’ 15%
 *   Position 3  â†’ 11%
 *   Position 4  â†’ 8%
 *   Position 5  â†’ 6%
 *   Position 6  â†’ 5%
 *   Position 7  â†’ 4%
 *   Position 8  â†’ 3%
 *   Position 9  â†’ 3%
 *   Position 10 â†’ 2.5%
 *   Below 10    â†’ decays to ~1%
 *
 * Formula:
 *   opportunityScore        = impressions Ã— (expectedCTR(pos=3) - currentCTR)
 *   estimatedNewClicks/mo   = opportunityScore
 *   estimatedRevenue/mo     = estimatedNewClicks Ã— storeConversionRate Ã— avgOrderValue
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeoGapService {

    // Standard industry CTR curve
    private static final double[] CTR_CURVE = {
            0.28,  // pos 1
            0.15,  // pos 2
            0.11,  // pos 3
            0.08,  // pos 4
            0.06,  // pos 5
            0.05,  // pos 6
            0.04,  // pos 7
            0.03,  // pos 8
            0.03,  // pos 9
            0.025, // pos 10
    };

    /** Minimum position to be in "opportunity zone" (too close to top = already winning) */
    private static final double MIN_POSITION = 8.0;
    /** Maximum position to consider (too far down = not viable quickly) */
    private static final double MAX_POSITION = 20.0;
    /** Minimum impressions threshold to filter noise */
    private static final int MIN_IMPRESSIONS = 200;

    private final StoreDataProvider dataProvider;

    public SeoGapResponse compute(String storeId) {
        log.info("Computing SEO gaps for store: {}", storeId);

        DummyStore store = dataProvider.getStore(storeId);
        List<DummyProduct> products = dataProvider.getProducts(storeId);
        List<DummyProductKeyword> allKeywords = dataProvider.getProductKeywords(storeId);

        // Build product lookup
        Map<String, DummyProduct> productMap = products.stream()
                .collect(Collectors.toMap(DummyProduct::getProductId, p -> p));

        // Compute store-level conversion rate from sessions
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(30);
        List<DummySessionEvent> sessions = dataProvider.getSessionEvents(storeId, from, to);

        long totalSessions = sessions.stream().mapToLong(DummySessionEvent::getSessions).sum();
        long totalPurchased = sessions.stream().mapToLong(DummySessionEvent::getPurchasedSessions).sum();
        double storeConversionRate = totalSessions > 0 ? (double) totalPurchased / totalSessions : 0.015;

        BigDecimal avgOrderValue = store.getAvgOrderValue();

        // Target CTR is position 3 = 11%
        double targetCtr = CTR_CURVE[2]; // index 2 = pos 3
        double targetPosition = 3.0;

        List<KeywordOpportunity> opportunities = allKeywords.stream()
                .filter(kw -> {
                    double pos = kw.getPosition().doubleValue();
                    return pos >= MIN_POSITION
                            && pos <= MAX_POSITION
                            && kw.getImpressions() >= MIN_IMPRESSIONS;
                })
                .map(kw -> {
                    DummyProduct product = productMap.get(kw.getProductId());
                    double currentCtr = kw.getCtr().doubleValue();
                    double currentPos = kw.getPosition().doubleValue();
                    int impressions = kw.getImpressions();

                    double opportunityScore = impressions * (targetCtr - currentCtr);
                    int estimatedNewClicks = (int) Math.max(0, opportunityScore);
                    BigDecimal estimatedRevenue = avgOrderValue
                            .multiply(BigDecimal.valueOf(estimatedNewClicks))
                            .multiply(BigDecimal.valueOf(storeConversionRate))
                            .setScale(2, RoundingMode.HALF_UP);

                    return KeywordOpportunity.builder()
                            .productId(kw.getProductId())
                            .productName(product != null ? product.getName() : kw.getProductId())
                            .query(kw.getQuery())
                            .currentPosition(round1(currentPos))
                            .currentCtr(round4(currentCtr))
                            .impressions(impressions)
                            .currentClicks(kw.getClicks())
                            .targetCtr(targetCtr)
                            .targetPosition(targetPosition)
                            .estimatedNewClicksPerMonth(estimatedNewClicks)
                            .estimatedRevenuePerMonth(estimatedRevenue)
                            .opportunityScore(round2(opportunityScore))
                            .build();
                })
                .sorted(Comparator.comparing(KeywordOpportunity::getEstimatedRevenuePerMonth).reversed())
                .collect(Collectors.toList());

        BigDecimal totalOpportunity = opportunities.stream()
                .map(KeywordOpportunity::getEstimatedRevenuePerMonth)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SeoGapResponse.builder()
                .storeId(storeId)
                .storeName(store.getStoreName())
                .dataSource(dataProvider.getDataSource())
                .totalOpportunityINR(totalOpportunity)
                .opportunities(opportunities)
                .build();
    }

    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }
}

