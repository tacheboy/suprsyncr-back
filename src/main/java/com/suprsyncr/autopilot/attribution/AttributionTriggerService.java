package com.suprsyncr.autopilot.attribution;

import com.suprsyncr.autopilot.domain.ProposedChangeEntity;
import com.suprsyncr.autopilot.repository.ProposedChangeRepository;
import com.suprsyncr.order.entity.Order;
import com.suprsyncr.order.entity.OrderItem;
import com.suprsyncr.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Deterministic trigger gate for Scenario 3.
 *
 * Question: "is there an approved/applied Suprsyncr change to this order's
 * product within the last N days?" If yes, the gate returns the list of
 * recent_changes to hand to the engine; if no, the caller records a
 * {@code GATE_SKIPPED} attribution row so the poller doesn't re-check.
 *
 * The gate is intentionally cheap — pure SQL, no model calls — so it's safe
 * to run on every recent order.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttributionTriggerService {

    private final ProposedChangeRepository proposedChangeRepository;
    private final ProductRepository productRepository;

    @Value("${attribution.gate-window-days:30}")
    private int gateWindowDays;

    /** What the gate produces, ready to ship to the engine. */
    public record GateResult(boolean passes,
                             Long productId,
                             List<Map<String, Object>> recentChanges) {

        public static GateResult skipped() {
            return new GateResult(false, null, List.of());
        }
    }

    public GateResult evaluate(Order order) {
        Optional<Long> productIdOpt = primaryProductId(order);
        if (productIdOpt.isEmpty()) {
            log.debug("attribution gate: order {} has no resolvable product",
                    order.getId());
            return GateResult.skipped();
        }
        Long productId = productIdOpt.get();

        // Proposals store the Shopify external id in shopify_entity_id
        // (sku "shopify-{id}" → the substring after "shopify-").
        // The gate must use the same key to match, otherwise it always misses.
        String entityId = productRepository.findById(productId)
                .map(p -> {
                    String sku = p.getSku();
                    return (sku != null && sku.startsWith("shopify-"))
                            ? sku.substring("shopify-".length())
                            : String.valueOf(productId);
                })
                .orElse(String.valueOf(productId));

        LocalDateTime since = LocalDateTime.now().minusDays(gateWindowDays);
        List<ProposedChangeEntity> changes =
                proposedChangeRepository.findRecentApprovedForEntity(entityId, since);
        if (changes.isEmpty()) {
            return GateResult.skipped();
        }

        List<Map<String, Object>> refs = changes.stream()
                .map(this::toRecentChangeRef)
                .toList();
        return new GateResult(true, productId, refs);
    }

    private Map<String, Object> toRecentChangeRef(ProposedChangeEntity pc) {
        LocalDateTime approvedOrApplied =
                pc.getAppliedAt() != null ? pc.getAppliedAt() : pc.getApprovedAt();
        String approvedAt = approvedOrApplied != null
                ? approvedOrApplied.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : "";

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("change_id", pc.getChangeId().toString());
        out.put("change_type", nullToEmpty(pc.getChangeType()));
        out.put("approved_at", approvedAt);
        // Compact human summary the manager can read without another tool call.
        StringBuilder summary = new StringBuilder()
                .append(nullToEmpty(pc.getChangeType()))
                .append(" on ").append(nullToEmpty(pc.getShopifyEntityType()))
                .append(" ").append(nullToEmpty(pc.getShopifyEntityId()));
        if (pc.getAgentReasoning() != null && !pc.getAgentReasoning().isBlank()) {
            summary.append(" — ").append(clip(pc.getAgentReasoning(), 180));
        }
        out.put("summary", summary.toString());
        return out;
    }

    private static Optional<Long> primaryProductId(Order order) {
        if (order.getItems() == null) return Optional.empty();
        for (OrderItem item : order.getItems()) {
            if (item.getProductVariant() != null
                    && item.getProductVariant().getProduct() != null
                    && item.getProductVariant().getProduct().getId() != null) {
                return Optional.of(item.getProductVariant().getProduct().getId());
            }
        }
        return Optional.empty();
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String clip(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }
}
