package com.suprsyncr.product.studio.controller;

import com.suprsyncr.product.studio.dto.CreateDraftRequest;
import com.suprsyncr.product.studio.dto.DraftDto;
import com.suprsyncr.product.studio.dto.PublishDraftRequest;
import com.suprsyncr.product.studio.service.ProductStudioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seller-facing REST surface for the Product Studio (Scenario 2).
 *
 *   POST /api/v1/products/studio/draft              — create draft, run engine
 *   GET  /api/v1/products/studio/drafts             — list drafts for a store
 *   GET  /api/v1/products/studio/drafts/{draftId}   — fetch a single draft
 *   POST /api/v1/products/studio/drafts/{draftId}/publish — publish to catalogue
 *
 * Auth: handled by the main {@code SecurityConfig} chain (JWT). This endpoint
 * is NOT in any permitAll list — sellers must be authenticated.
 */
@RestController
@RequestMapping("/api/v1/products/studio")
@RequiredArgsConstructor
@Slf4j
public class ProductStudioController {

    private final ProductStudioService studioService;

    @PostMapping("/draft")
    public ResponseEntity<DraftDto> createDraft(@Valid @RequestBody CreateDraftRequest req) {
        log.info("studio createDraft store={} claimedTitle={}",
                req.getStoreId(), req.getClaimedTitle());
        DraftDto dto = studioService.createDraft(req);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/drafts/{draftId}")
    public ResponseEntity<DraftDto> getDraft(@PathVariable UUID draftId) {
        return studioService.getDraft(draftId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/drafts")
    public ResponseEntity<List<DraftDto>> listDrafts(@RequestParam("storeId") String storeId) {
        return ResponseEntity.ok(studioService.listDrafts(storeId));
    }

    @PostMapping("/drafts/{draftId}/publish")
    public ResponseEntity<?> publishDraft(@PathVariable UUID draftId,
                                          @Valid @RequestBody PublishDraftRequest req) {
        try {
            DraftDto dto = studioService.publishDraft(draftId, req);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}
