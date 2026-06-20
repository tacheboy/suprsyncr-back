package com.suprsyncr.analytics.service;

import com.suprsyncr.analytics.domain.DummyProduct;
import com.suprsyncr.analytics.domain.DummyStore;
import com.suprsyncr.analytics.dto.ProductHealthResponse;
import com.suprsyncr.analytics.dto.ProductHealthResponse.ActionQueueItem;
import com.suprsyncr.analytics.dto.ProductHealthResponse.ProductQuadrant;
import com.suprsyncr.analytics.provider.StoreDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 2 â€” Product Health Classifier (pure computation, no AI).
 *
 * Each product is scored on two axes:
 *   trafficPercentile    = rank of pageViews among all store products (0â€“1)
 *   conversionPercentile = rank of conversionRate among all store products (0â€“1)
 *
 * Quadrant classification (threshold = 0.5):
 *   HIGH traffic + LOW conversion   â†’ LISTING_PROBLEM  â†’ fix listing (Service 1)
 *   HIGH traffic + HIGH conversion  â†’ WINNER           â†’ promote / expand
 *   LOW  traffic + HIGH conversion  â†’ SEO_PROBLEM      â†’ improve SEO (Service 1)
 *   LOW  traffic + LOW conversion   â†’ WRONG_PLATFORM   â†’ try other channels (Service 3)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductHealthService {

    private static final double HIGH_THRESHOLD = 0.5;

    private final StoreDataProvider dataProvider;

    public ProductHealthResponse compute(String storeId) {
        log.info("Computing product health for store: {}", storeId);

        DummyStore store = dataProvider.getStore(storeId);
        List<DummyProduct> products = dataProvider.getProducts(storeId);

        if (products.isEmpty()) {
            return ProductHealthResponse.builder()
                    .storeId(storeId)
                    .storeName(store.getStoreName())
                    .dataSource(dataProvider.getDataSource())
                    .products(List.of())
                    .actionQueue(List.of())
                    .build();
        }

        // â”€â”€â”€ Compute conversion rates â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        record ProductWithMetrics(DummyProduct p, double conversionRate) {}
        List<ProductWithMetrics> withRates = products.stream()
                .map(p -> {
                    double cr = p.getMonthlyPageViews() > 0
                            ? (double) p.getPurchaseCount() / p.getMonthlyPageViews()
                            : 0.0;
                    return new ProductWithMetrics(p, cr);
                })
                .toList();

        // â”€â”€â”€ Percentile ranks â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        List<Integer> sortedViews = withRates.stream()
                .map(x -> x.p().getMonthlyPageViews())
                .sorted()
                .collect(Collectors.toList());
        List<Double> sortedCr = withRates.stream()
                .map(ProductWithMetrics::conversionRate)
                .sorted()
                .collect(Collectors.toList());

        int n = products.size();

        List<ProductQuadrant> quadrants = withRates.stream()
                .map(x -> {
                    DummyProduct p = x.p();
                    double cr = x.conversionRate();

                    int viewRank = sortedViews.indexOf(p.getMonthlyPageViews());
                    double trafficPct = n > 1 ? (double) viewRank / (n - 1) : 0.5;

                    int crRank = sortedCr.indexOf(cr);
                    double convPct = n > 1 ? (double) crRank / (n - 1) : 0.5;

                    boolean highTraffic = trafficPct >= HIGH_THRESHOLD;
                    boolean highConversion = convPct >= HIGH_THRESHOLD;

                    String quadrant;
                    String reason;
                    String ctaLabel;
                    String ctaAction;

                    if (highTraffic && highConversion) {
                        quadrant = "WINNER";
                        reason = String.format("Top %d%% traffic, top %d%% conversion â€” this product is doing everything right.",
                                (int)((1 - trafficPct) * 100 + 1), (int)((1 - convPct) * 100 + 1));
                        ctaLabel = "Expand to More Platforms";
                        ctaAction = "SUGGEST_PLATFORMS";
                    } else if (highTraffic && !highConversion) {
                        quadrant = "LISTING_PROBLEM";
                        reason = String.format("Lots of visitors (top %d%% traffic) but only %.1f%% convert. Your listing isn't closing the sale.",
                                (int)((1 - trafficPct) * 100 + 1), cr * 100);
                        ctaLabel = "Fix Listing";
                        ctaAction = "OPTIMIZE_LISTING";
                    } else if (!highTraffic && highConversion) {
                        quadrant = "SEO_PROBLEM";
                        reason = String.format("%.1f%% of visitors buy â€” great product â€” but only %d people find it per month. You need more visibility.",
                                cr * 100, p.getMonthlyPageViews());
                        ctaLabel = "Improve SEO";
                        ctaAction = "OPTIMIZE_SEO";
                    } else {
                        quadrant = "WRONG_PLATFORM";
                        reason = String.format("Low traffic AND low conversion (%.1f%%). This product may not be right for this channel.",
                                cr * 100);
                        ctaLabel = "Find Better Platform";
                        ctaAction = "SUGGEST_PLATFORMS";
                    }

                    // Estimate daily revenue impact for action queue ranking
                    int atc = p.getAddToCartCount();
                    int bought = p.getPurchaseCount();
                    int abandoned = Math.max(0, atc - bought);
                    BigDecimal dailyImpact = store.getAvgOrderValue()
                            .multiply(BigDecimal.valueOf(abandoned))
                            .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);

                    double abandonRate = atc > 0 ? (double) abandoned / atc : 0.0;

                    return ProductQuadrant.builder()
                            .productId(p.getProductId())
                            .name(p.getName())
                            .category(p.getCategory())
                            .price(p.getPrice())
                            .pageViews(p.getMonthlyPageViews())
                            .conversionRate(round3(cr))
                            .abandonmentRate(round2(abandonRate))
                            .trafficPercentile(round2(trafficPct))
                            .conversionPercentile(round2(convPct))
                            .quadrant(quadrant)
                            .reason(reason)
                            .ctaLabel(ctaLabel)
                            .ctaAction(ctaAction)
                            .estimatedDailyImpactINR(dailyImpact)
                            .build();
                })
                .sorted(Comparator.comparing(ProductQuadrant::getEstimatedDailyImpactINR).reversed())
                .collect(Collectors.toList());

        // â”€â”€â”€ Action Queue (top 5 by daily impact) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        List<ActionQueueItem> actionQueue = quadrants.stream()
                .filter(q -> !q.getQuadrant().equals("WINNER"))
                .limit(5)
                .map((ProductQuadrant q) -> {
                    String desc = buildActionDescription(q);
                    return ActionQueueItem.builder()
                            .rank(0)  // will be set by re-rank loop below
                            .productId(q.getProductId())
                            .productName(q.getName())
                            .action(q.getQuadrant())
                            .description(desc)
                            .estimatedDailyImpactINR(q.getEstimatedDailyImpactINR())
                            .ctaLabel(q.getCtaLabel())
                            .ctaAction(q.getCtaAction())
                            .build();
                })
                .collect(Collectors.toList());

        // Re-rank sequentially
        for (int i = 0; i < actionQueue.size(); i++) {
            actionQueue.get(i).setRank(i + 1);
        }

        return ProductHealthResponse.builder()
                .storeId(storeId)
                .storeName(store.getStoreName())
                .dataSource(dataProvider.getDataSource())
                .products(quadrants)
                .actionQueue(actionQueue)
                .build();
    }

    private String buildActionDescription(ProductQuadrant q) {
        return switch (q.getQuadrant()) {
            case "LISTING_PROBLEM" -> String.format(
                    "Fix listing for \"%s\" â†’ estimated recovery â‚¹%.0f/day",
                    q.getName(), q.getEstimatedDailyImpactINR());
            case "SEO_PROBLEM" -> String.format(
                    "Improve SEO for \"%s\" â†’ %d monthly visitors, low visibility",
                    q.getName(), q.getPageViews());
            case "WRONG_PLATFORM" -> String.format(
                    "Try different platform for \"%s\" â†’ wrong audience on current channel",
                    q.getName());
            default -> q.getReason();
        };
    }

    private double round2(double val) { return Math.round(val * 100.0) / 100.0; }
    private double round3(double val) { return Math.round(val * 1000.0) / 1000.0; }
}

