package com.suprsyncr.autopilot.attribution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.autopilot.attribution.dto.AttributionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seller-facing REST surface for Scenario 3 attribution.
 *
 *   POST /api/v1/attribution/analyze/{orderId}    — manual trigger (demo button)
 *   GET  /api/v1/attribution/store/{storeId}      — Impact Lab list, newest first
 *   GET  /api/v1/attribution/{attributionId}      — single attribution + proposals
 *
 * Auth is the default JWT chain — these endpoints are NOT in any permitAll list.
 */
@RestController
@RequestMapping("/api/v1/attribution")
@RequiredArgsConstructor
@Slf4j
public class AttributionController {

    private final AttributionService attributionService;
    private final ObjectMapper objectMapper;

    @PostMapping("/analyze/{orderId}")
    public ResponseEntity<?> analyzeOrder(@PathVariable Long orderId) {
        try {
            Attribution row = attributionService.analyzeOrder(orderId);
            return ResponseEntity.ok(AttributionDto.from(row, objectMapper));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<AttributionDto>> listForStore(@PathVariable String storeId) {
        List<AttributionDto> out = attributionService.listForStore(storeId).stream()
                .map(a -> AttributionDto.from(a, objectMapper))
                .toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{attributionId}")
    public ResponseEntity<AttributionDto> get(@PathVariable UUID attributionId) {
        return attributionService.get(attributionId)
                .map(a -> AttributionDto.from(a, objectMapper))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
