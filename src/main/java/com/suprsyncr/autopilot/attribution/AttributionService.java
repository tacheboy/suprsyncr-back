package com.suprsyncr.autopilot.attribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.order.entity.Order;
import com.suprsyncr.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * Drives Scenario 3 end-to-end on the Spring side:
 *
 *   1. Read trigger gate (cheap SQL, no model calls).
 *   2. Persist a PENDING Attribution row via AttributionPersistenceService,
 *      or GATE_SKIPPED if the gate declines.
 *   3. Call the engine's POST /engine/order-attribution.
 *   4. Persist verdict + propagated proposals + telemetry via the persistence service.
 *   5. Credit realised revenue back to the causal change's ChangeImpact row.
 *
 * Persistence is intentionally delegated to AttributionPersistenceService so
 * Spring's @Transactional proxy is active on every DB write. Calling those
 * methods via this. in a non-transactional method would bypass the proxy.
 *
 * The order is loaded with a fetch-join (findByIdWithDetails) to eagerly
 * initialise items, productVariants, and platform before any session closes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttributionService {

    private final AttributionRepository attributionRepository;
    private final AttributionTriggerService triggerService;
    private final OrderRepository orderRepository;
    private final AttributionPersistenceService persistenceService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${attribution.engine-url:${autopilot.agent-service-url:http://localhost:8081}}")
    private String engineUrl;

    @Value("${attribution.budget-inr:5.0}")
    private double budgetInr;

    @Value("${attribution.max-similar:8}")
    private int maxSimilar;

    // ====== entry points =====================================================

    /**
     * Run attribution for one order. Idempotent: a second call for the same
     * order returns the existing Attribution row without re-running the engine.
     */
    public Attribution analyzeOrder(Long orderId) {
        Optional<Attribution> existing = attributionRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.debug("attribution: order {} already processed, status={}",
                    orderId, existing.get().getStatus());
            return existing.get();
        }

        // Fetch-join eagerly loads items → productVariant → product + platform
        // so the trigger gate can traverse those associations without an open session.
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));

        AttributionTriggerService.GateResult gate = triggerService.evaluate(order);
        if (!gate.passes()) {
            return persistenceService.persistGateSkipped(order);
        }

        Attribution row = persistenceService.persistPending(order, gate.productId());
        try {
            JsonNode resp = callEngine(row, order, gate);
            return persistenceService.persistEngineResult(row.getAttributionId(), order, resp);
        } catch (Exception e) {
            log.warn("attribution engine call failed for order {}: {}", orderId, e.getMessage());
            return persistenceService.markFailed(row.getAttributionId(), e.getMessage());
        }
    }

    /** Impact Lab list — newest first, all attempts including GATE_SKIPPED. */
    public List<Attribution> listForStore(String storeId) {
        return attributionRepository.findByStoreIdOrderByTriggeredAtDesc(storeId);
    }

    public Optional<Attribution> get(UUID attributionId) {
        return attributionRepository.findById(attributionId);
    }

    // ====== engine call ======================================================

    private JsonNode callEngine(Attribution row, Order order,
                                AttributionTriggerService.GateResult gate) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("run_id", row.getAttributionId().toString());
        payload.put("order_id", String.valueOf(order.getId()));
        payload.put("store_id", row.getStoreId());
        payload.put("product_id", String.valueOf(gate.productId()));
        payload.put("order_revenue_inr",
                row.getOrderRevenueInr() != null ? row.getOrderRevenueInr() : BigDecimal.ZERO);
        payload.put("recent_changes", gate.recentChanges());
        payload.put("budget_inr", budgetInr);
        payload.put("max_similar", maxSimilar);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                engineUrl + "/engine/order-attribution", req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException(
                    "engine /order-attribution returned " + resp.getStatusCode());
        }
        return objectMapper.readTree(resp.getBody());
    }
}
