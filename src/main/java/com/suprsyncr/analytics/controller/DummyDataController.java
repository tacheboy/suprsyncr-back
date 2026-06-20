package com.suprsyncr.analytics.controller;

import com.suprsyncr.analytics.domain.DummyDailyOrder;
import com.suprsyncr.analytics.domain.DummyProduct;
import com.suprsyncr.analytics.domain.DummyProductKeyword;
import com.suprsyncr.analytics.domain.DummySessionEvent;
import com.suprsyncr.analytics.domain.DummyStore;
import com.suprsyncr.analytics.dto.StoreSummaryResponse;
import com.suprsyncr.analytics.provider.StoreDataProvider;
import com.suprsyncr.analytics.repository.DummyStoreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 *  TEMPORARY DEV CONTROLLER â€” @Profile("dev") ONLY
 *  This controller CANNOT run in production.
 *  It serves dummy D2C store data for analytics development.
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 *
 * All responses carry dataSource: "dummy" so the frontend
 * can show the "Demo Mode" banner.
 */
@RestController
@RequestMapping("/api/dummy")
@Profile("dev")
@RequiredArgsConstructor
@Tag(name = "Dummy Data (Dev Only)", description = "Temporary dev endpoints serving dummy D2C store data for analytics development. Profile: dev only.")
public class DummyDataController {

    // Only storeRepository is injected directly â€” needed for /stores list.
    // All other data access goes through StoreDataProvider (abstraction layer).
    private final DummyStoreRepository storeRepository;
    private final StoreDataProvider dataProvider;

    @GetMapping("/stores")
    @Operation(summary = "List Dummy Stores", description = "Returns all dummy D2C stores available for analytics testing")
    public ResponseEntity<Map<String, Object>> listStores() {
        List<DummyStore> stores = storeRepository.findAll();
        return ok(Map.of(
                "stores", stores,
                "count", stores.size()
        ));
    }

    @GetMapping("/store/{storeId}/summary")
    @Operation(summary = "Store Summary", description = "Top-level store metrics including overall funnel performance")
    public ResponseEntity<Map<String, Object>> getStoreSummary(@PathVariable String storeId) {
        DummyStore store = dataProvider.getStore(storeId);
        List<DummyProduct> products = dataProvider.getProducts(storeId);

        int totalPageViews = products.stream().mapToInt(DummyProduct::getMonthlyPageViews).sum();
        int totalPurchases = products.stream().mapToInt(DummyProduct::getPurchaseCount).sum();
        BigDecimal totalRevenue = products.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getPurchaseCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalAtc = products.stream().mapToInt(DummyProduct::getAddToCartCount).sum();
        double abandonRate = totalAtc > 0 ? (double)(totalAtc - totalPurchases) / totalAtc : 0;
        double convRate = totalPageViews > 0 ? (double) totalPurchases / totalPageViews : 0;

        StoreSummaryResponse summary = StoreSummaryResponse.builder()
                .storeId(storeId)
                .storeName(store.getStoreName())
                .category(store.getCategory())
                .dataSource("dummy")
                .monthlyTraffic(store.getMonthlyTraffic())
                .avgOrderValue(store.getAvgOrderValue())
                .primaryTrafficSource(store.getPrimaryTrafficSource())
                .totalProducts(products.size())
                .totalMonthlyRevenue(totalRevenue.setScale(2, RoundingMode.HALF_UP))
                .overallConversionRate(round3(convRate))
                .overallAbandonmentRate(round2(abandonRate))
                .totalMonthlyOrders(totalPurchases)
                .build();

        return ok(Map.of("summary", summary));
    }

    @GetMapping("/store/{storeId}/products")
    @Operation(summary = "Store Products", description = "All products with full funnel metrics")
    public ResponseEntity<Map<String, Object>> getProducts(@PathVariable String storeId) {
        List<DummyProduct> products = dataProvider.getProducts(storeId);
        return ok(Map.of("products", products, "count", products.size()));
    }

    @GetMapping("/store/{storeId}/sessions")
    @Operation(summary = "Session Events", description = "Daily session funnel data for the last 30 days")
    public ResponseEntity<Map<String, Object>> getSessions(@PathVariable String storeId) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(30);
        List<DummySessionEvent> events = dataProvider.getSessionEvents(storeId, from, to);
        return ok(Map.of("sessions", events, "count", events.size(), "range", from + " to " + to));
    }

    @GetMapping("/store/{storeId}/orders")
    @Operation(summary = "Order History", description = "Order history for the last 30 days")
    public ResponseEntity<Map<String, Object>> getOrders(@PathVariable String storeId) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(30);
        List<DummyDailyOrder> orders = dataProvider.getOrders(storeId, from, to);
        return ok(Map.of("orders", orders, "count", orders.size(), "range", from + " to " + to));
    }

    @GetMapping("/store/{storeId}/seo")
    @Operation(summary = "SEO Data", description = "Keyword/position/CTR data per product")
    public ResponseEntity<Map<String, Object>> getSeoData(@PathVariable String storeId) {
        List<DummyProductKeyword> keywords = dataProvider.getProductKeywords(storeId);
        // Group by product
        Map<String, List<DummyProductKeyword>> byProduct = keywords.stream()
                .collect(Collectors.groupingBy(DummyProductKeyword::getProductId));
        return ok(Map.of("keywords", byProduct, "totalKeywords", keywords.size()));
    }

    // â”€â”€â”€ Envelope builder â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private ResponseEntity<Map<String, Object>> ok(Map<String, Object> data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("dataSource", "dummy");
        response.putAll(data);
        return ResponseEntity.ok(response);
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }
}

