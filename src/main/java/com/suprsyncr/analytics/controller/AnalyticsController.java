package com.suprsyncr.analytics.controller;

import com.suprsyncr.analytics.dto.ProductHealthResponse;
import com.suprsyncr.analytics.dto.RevenueLeakResponse;
import com.suprsyncr.analytics.dto.SeoGapResponse;
import com.suprsyncr.analytics.service.AnalyticsOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Analytics API â€” serves computed analytics insights.
 *
 * All endpoints follow the contract:
 *   { "success": true, "storeId": "...", "dataSource": "dummy|live", "data": { ... } }
 *
 * Read paths always serve from the latest AnalyticsSnapshot (never compute on read).
 * The /refresh endpoints trigger a forced recompute + Gemini enrichment.
 *
 * No authentication required for now (dev mode).
 * Phase 5: add @PreAuthorize once real user-store mapping is implemented.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Deep analytics engine â€” Revenue Leak, Product Health, SEO Gap, Gemini Insights")
public class AnalyticsController {

    private final AnalyticsOrchestrator orchestrator;

    // â”€â”€â”€ Revenue Leak (Phase 1) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/{storeId}/revenue-leak")
    @Operation(
            summary = "Revenue Leak",
            description = "Shows total rupee revenue lost to cart abandonment and checkout drop, " +
                    "broken down by product and traffic source. Served from snapshot unless force-refreshed."
    )
    public ResponseEntity<Map<String, Object>> getRevenueLeak(
            @PathVariable String storeId,
            @RequestParam(defaultValue = "false") boolean refresh) {

        RevenueLeakResponse data = refresh
                ? orchestrator.recomputeRevenueLeak(storeId)
                : orchestrator.getRevenueLeak(storeId);

        return envelope(storeId, data.getDataSource(), data);
    }

    // â”€â”€â”€ Product Health Matrix (Phase 2) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/{storeId}/product-health")
    @Operation(
            summary = "Product Health Matrix",
            description = "Classifies every product into one of 4 quadrants: WINNER, LISTING_PROBLEM, " +
                    "SEO_PROBLEM, WRONG_PLATFORM. Includes action queue sorted by estimated revenue impact."
    )
    public ResponseEntity<Map<String, Object>> getProductHealth(
            @PathVariable String storeId,
            @RequestParam(defaultValue = "false") boolean refresh) {

        ProductHealthResponse data = refresh
                ? orchestrator.recomputeProductHealth(storeId)
                : orchestrator.getProductHealth(storeId);

        return envelope(storeId, data.getDataSource(), data);
    }

    // â”€â”€â”€ SEO Gap Analyzer (Phase 3) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/{storeId}/seo-gaps")
    @Operation(
            summary = "SEO Gap Analyzer",
            description = "Identifies keywords in positions 8â€“20 with meaningful impressions and calculates " +
                    "the revenue opportunity if they moved to position 3. Uses standard industry CTR curves."
    )
    public ResponseEntity<Map<String, Object>> getSeoGaps(
            @PathVariable String storeId,
            @RequestParam(defaultValue = "false") boolean refresh) {

        SeoGapResponse data = refresh
                ? orchestrator.recomputeSeoGaps(storeId)
                : orchestrator.getSeoGaps(storeId);

        return envelope(storeId, data.getDataSource(), data);
    }

    // â”€â”€â”€ Force-refresh all analytics for a store â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PostMapping("/{storeId}/refresh")
    @Operation(
            summary = "Refresh All Analytics",
            description = "Triggers a full recompute of all analytics types for a store: Revenue Leak, " +
                    "Product Health, SEO Gap. Each is enriched with a Gemini insight and persisted to snapshot."
    )
    public ResponseEntity<Map<String, Object>> refreshAll(@PathVariable String storeId) {
        orchestrator.recomputeAll(storeId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "storeId", storeId,
                "message", "All analytics recomputed and persisted"
        ));
    }

    // â”€â”€â”€ Envelope â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private ResponseEntity<Map<String, Object>> envelope(String storeId, String dataSource, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("storeId", storeId);
        body.put("dataSource", dataSource != null ? dataSource : "dummy");
        body.put("data", data);
        return ResponseEntity.ok(body);
    }
}

