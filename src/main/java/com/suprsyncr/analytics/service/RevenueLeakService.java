package com.suprsyncr.analytics.service;

import com.suprsyncr.analytics.domain.DummyDailyOrder;
import com.suprsyncr.analytics.domain.DummyProduct;
import com.suprsyncr.analytics.domain.DummySessionEvent;
import com.suprsyncr.analytics.domain.DummyStore;
import com.suprsyncr.analytics.dto.RevenueLeakResponse;
import com.suprsyncr.analytics.dto.RevenueLeakResponse.ProductLeakBreakdown;
import com.suprsyncr.analytics.dto.RevenueLeakResponse.SourceLeakBreakdown;
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
 * Phase 1 â€” Revenue Leak Calculator (pure computation, no AI).
 *
 * Formula:
 *   cartAbandonmentLoss  = sum(addToCartSessions - purchasedSessions) Ã— avgOrderValue
 *   checkoutDropLoss     = sum(checkoutInitSessions - purchasedSessions) Ã— avgOrderValue
 *   totalRevenueLeak     = cartAbandonmentLoss + checkoutDropLoss
 *
 * Uses session events for the per-source breakdown,
 * and product funnel counts for the per-product breakdown.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueLeakService {

    private final StoreDataProvider dataProvider;

    public RevenueLeakResponse compute(String storeId) {
        log.info("Computing revenue leak for store: {}", storeId);

        DummyStore store = dataProvider.getStore(storeId);
        List<DummyProduct> products = dataProvider.getProducts(storeId);

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(30);
        List<DummySessionEvent> sessions = dataProvider.getSessionEvents(storeId, from, to);

        BigDecimal avgOrderValue = store.getAvgOrderValue();

        // â”€â”€â”€ Per-product breakdown â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        List<ProductLeakBreakdown> byProduct = products.stream()
                .map(p -> {
                    int atc = p.getAddToCartCount();
                    int bought = p.getPurchaseCount();
                    int abandoned = Math.max(0, atc - bought);
                    BigDecimal leak = avgOrderValue.multiply(BigDecimal.valueOf(abandoned));
                    double abandonRate = atc > 0 ? (double) abandoned / atc : 0.0;

                    return ProductLeakBreakdown.builder()
                            .productId(p.getProductId())
                            .name(p.getName())
                            .leakINR(leak.setScale(2, RoundingMode.HALF_UP))
                            .abandonmentRate(round2(abandonRate))
                            .addToCartCount(atc)
                            .purchaseCount(bought)
                            .build();
                })
                .sorted(Comparator.comparing(ProductLeakBreakdown::getLeakINR).reversed())
                .collect(Collectors.toList());

        // â”€â”€â”€ Per-source breakdown â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Map<String, int[]> sourceAgg = new LinkedHashMap<>();
        for (DummySessionEvent evt : sessions) {
            int[] agg = sourceAgg.computeIfAbsent(evt.getSource(), k -> new int[4]);
            agg[0] += evt.getSessions();
            agg[1] += evt.getAddToCartSessions();
            agg[2] += evt.getCheckoutInitSessions();
            agg[3] += evt.getPurchasedSessions();
        }

        List<SourceLeakBreakdown> bySource = sourceAgg.entrySet().stream()
                .map(e -> {
                    String src = e.getKey();
                    int[] agg = e.getValue();
                    int atc = agg[1];
                    int purchased = agg[3];
                    int abandoned = Math.max(0, atc - purchased);
                    BigDecimal leak = avgOrderValue.multiply(BigDecimal.valueOf(abandoned));
                    double abandonRate = atc > 0 ? (double) abandoned / atc : 0.0;

                    return SourceLeakBreakdown.builder()
                            .source(src)
                            .leakINR(leak.setScale(2, RoundingMode.HALF_UP))
                            .abandonmentRate(round2(abandonRate))
                            .sessions(agg[0])
                            .purchasedSessions(purchased)
                            .build();
                })
                .sorted(Comparator.comparing(SourceLeakBreakdown::getLeakINR).reversed())
                .collect(Collectors.toList());

        // â”€â”€â”€ Totals â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        BigDecimal cartLoss = byProduct.stream()
                .map(ProductLeakBreakdown::getLeakINR)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Checkout drop = sessions that started checkout but didn't buy
        long checkoutDropSessions = sessions.stream()
                .mapToLong(e -> Math.max(0, e.getCheckoutInitSessions() - e.getPurchasedSessions()))
                .sum();
        BigDecimal checkoutLoss = avgOrderValue
                .multiply(BigDecimal.valueOf(checkoutDropSessions))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalLeak = cartLoss.add(checkoutLoss);

        // Overall abandonment rate across all sessions
        long totalAtcSessions = sessions.stream().mapToLong(DummySessionEvent::getAddToCartSessions).sum();
        long totalPurchased = sessions.stream().mapToLong(DummySessionEvent::getPurchasedSessions).sum();
        double overallAbandonment = totalAtcSessions > 0
                ? (double)(totalAtcSessions - totalPurchased) / totalAtcSessions : 0.0;

        return RevenueLeakResponse.builder()
                .storeId(storeId)
                .storeName(store.getStoreName())
                .dataSource(dataProvider.getDataSource())
                .totalLeakINR(totalLeak)
                .cartAbandonmentLossINR(cartLoss.setScale(2, RoundingMode.HALF_UP))
                .checkoutDropLossINR(checkoutLoss)
                .overallAbandonmentRate(round2(overallAbandonment))
                .byProduct(byProduct)
                .bySource(bySource)
                .build();
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}

