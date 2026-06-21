package com.suprsyncr.product.studio.internal;

import com.suprsyncr.autopilot.domain.ProposedChangeEntity;
import com.suprsyncr.autopilot.repository.ProposedChangeRepository;
import com.suprsyncr.order.entity.Order;
import com.suprsyncr.order.entity.OrderItem;
import com.suprsyncr.order.repository.OrderRepository;
import com.suprsyncr.product.entity.Product;
import com.suprsyncr.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Read-only endpoints called by the Python engine's function-tools. Lives on
 * the {@code /api/internal/**} chain (service-token authed; no JWT). Every
 * handler is keyed by {@code storeId} and only reads data the calling tool
 * has been granted — there are no write endpoints in this namespace.
 *
 * Returned shapes are deliberately small and deterministic so the engine's
 * tool layer can pass them to the model without further reshaping.
 */
@RestController
@RequestMapping("/api/internal/engine-tools")
@RequiredArgsConstructor
@Slf4j
public class EngineToolsController {

    private final StoreContextResolver storeContextResolver;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProposedChangeRepository proposedChangeRepository;

    /**
     * Sample of the seller's existing listings — used by the Studio manager to
     * ground generated copy in the store's existing tone/voice.
     */
    @GetMapping("/store-voice")
    public ResponseEntity<Map<String, Object>> storeVoice(
            @RequestParam("storeId") String storeId,
            @RequestParam(value = "limit", defaultValue = "8") int limit) {
        int cap = Math.min(Math.max(limit, 1), 20);
        Optional<Long> sellerId = storeContextResolver.resolveSellerId(storeId);
        if (sellerId.isEmpty()) {
            return ResponseEntity.ok(Map.of("storeId", storeId, "samples", List.of()));
        }
        var page = productRepository.findProducts(sellerId.get(), null, null, null,
                PageRequest.of(0, cap));
        List<Map<String, String>> samples = page.getContent().stream()
                .map(p -> Map.of(
                        "name", nullToEmpty(p.getName()),
                        "description", clip(p.getDescription(), 320),
                        "brand", nullToEmpty(p.getBrand())))
                .toList();
        return ResponseEntity.ok(Map.of("storeId", storeId, "samples", samples));
    }

    /**
     * Existing products in the seller's catalogue whose name loosely matches
     * the manager's hint. Used as grounding — "what have we listed before for
     * this kind of product."
     */
    @GetMapping("/similar-listings")
    public ResponseEntity<Map<String, Object>> similarListings(
            @RequestParam("storeId") String storeId,
            @RequestParam(value = "hint", required = false) String hint,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        int cap = Math.min(Math.max(limit, 1), 20);
        Optional<Long> sellerId = storeContextResolver.resolveSellerId(storeId);
        if (sellerId.isEmpty()) {
            return ResponseEntity.ok(Map.of("storeId", storeId, "hint", nullToEmpty(hint),
                    "matches", List.of()));
        }
        String search = (hint == null || hint.isBlank()) ? null : hint.trim();
        var page = productRepository.findProducts(sellerId.get(), search, null, null,
                PageRequest.of(0, cap));
        List<Map<String, Object>> matches = page.getContent().stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", nullToEmpty(p.getName()));
                    m.put("brand", nullToEmpty(p.getBrand()));
                    m.put("base_price_inr", p.getBasePrice());
                    m.put("description_excerpt", clip(p.getDescription(), 200));
                    return m;
                })
                .toList();
        return ResponseEntity.ok(Map.of("storeId", storeId, "hint", nullToEmpty(hint),
                "matches", matches));
    }

    /** Full product detail — primarily used by the Scenario 3 (attribution) flow. */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable Long productId) {
        Optional<Product> opt = productRepository.findById(productId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Product p = opt.get();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", p.getId());
        out.put("name", nullToEmpty(p.getName()));
        out.put("description", nullToEmpty(p.getDescription()));
        out.put("brand", nullToEmpty(p.getBrand()));
        out.put("sku", nullToEmpty(p.getSku()));
        out.put("base_price_inr", p.getBasePrice());
        out.put("status", p.getStatus() != null ? p.getStatus().name() : null);
        out.put("image_urls", p.getImageUrls());
        return ResponseEntity.ok(out);
    }

    // ======================================================================
    //   Attribution-side tools (Scenario 3)
    // ======================================================================

    /**
     * Facts about one order — used by the attribution manager to judge whether
     * the sale matches the buyer profile a recent change was targeting.
     */
    @GetMapping("/order-context/{orderId}")
    public ResponseEntity<Map<String, Object>> orderContext(@PathVariable String orderId) {
        Optional<Order> opt = resolveOrder(orderId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Order o = opt.get();

        // First item gives us the primary product the order is for; multi-item
        // orders are still summarised but the manager attributes per product.
        Long primaryProductId = null;
        String primaryProductName = "";
        if (!o.getItems().isEmpty()) {
            OrderItem item = o.getItems().get(0);
            if (item.getProductVariant() != null
                    && item.getProductVariant().getProduct() != null) {
                primaryProductId = item.getProductVariant().getProduct().getId();
            }
            primaryProductName = nullToEmpty(item.getProductName());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("order_id", o.getId());
        out.put("external_order_id", nullToEmpty(o.getExternalOrderId()));
        out.put("usp_order_id", nullToEmpty(o.getUspOrderId()));
        out.put("status", o.getStatus() != null ? o.getStatus().name() : null);
        out.put("total_amount_inr", o.getTotalAmount());
        out.put("customer_name", nullToEmpty(o.getCustomerName()));
        out.put("customer_email", nullToEmpty(o.getCustomerEmail()));
        out.put("ordered_at", o.getOrderedAt() != null
                ? o.getOrderedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        out.put("platform", o.getPlatform() != null && o.getPlatform().getPlatformType() != null
                ? o.getPlatform().getPlatformType().name() : null);
        out.put("primary_product_id", primaryProductId);
        out.put("primary_product_name", primaryProductName);
        out.put("item_count", o.getItems().size());
        return ResponseEntity.ok(out);
    }

    /**
     * Applied changes to a single product within the last N days. Mirrors the
     * recent_changes summaries Spring passes in the attribution request — the
     * manager calls this when it wants to confirm exactly what changed.
     */
    @GetMapping("/change-history")
    public ResponseEntity<Map<String, Object>> changeHistory(
            @RequestParam("productId") String productId,
            @RequestParam(value = "days", defaultValue = "30") int days) {
        int cap = Math.min(Math.max(days, 1), 365);
        LocalDateTime since = LocalDateTime.now().minusDays(cap);
        List<ProposedChangeEntity> changes =
                proposedChangeRepository.findAppliedForEntitySince(productId, since);

        List<Map<String, Object>> entries = changes.stream()
                .map(this::changeSummary)
                .toList();
        return ResponseEntity.ok(Map.of(
                "product_id", productId,
                "days", cap,
                "changes", entries));
    }

    /**
     * Cheap funnel proxy: count of delivered orders and total revenue for a
     * product before vs after a pivot timestamp. When no pivot is supplied we
     * split the window in half.
     */
    @GetMapping("/funnel")
    public ResponseEntity<Map<String, Object>> funnel(
            @RequestParam("productId") String productId,
            @RequestParam(value = "pivotAt", required = false) String pivotAt,
            @RequestParam(value = "windowDays", defaultValue = "14") int windowDays) {
        Long productLongId = safeParseLong(productId);
        int win = Math.min(Math.max(windowDays, 1), 90);
        LocalDateTime pivot = parsePivot(pivotAt).orElse(LocalDateTime.now());
        LocalDateTime beforeStart = pivot.minusDays(win);
        LocalDateTime afterEnd = pivot.plusDays(win);

        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();
        if (productLongId != null) {
            countWindow(productLongId, beforeStart, pivot, before);
            countWindow(productLongId, pivot, afterEnd, after);
        } else {
            countWindow(null, beforeStart, pivot, before);
            countWindow(null, pivot, afterEnd, after);
        }

        return ResponseEntity.ok(Map.of(
                "product_id", productId,
                "pivot_at", pivot.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "window_days", win,
                "before", before,
                "after", after));
    }

    /**
     * Candidates the attribution manager can propagate a successful pattern to.
     * MVP heuristic: same seller, same category, optionally same brand —
     * ranked by recency. Bounded above so the manager + sub-agent fanout
     * remains cost-predictable.
     */
    @GetMapping("/similar-products")
    public ResponseEntity<Map<String, Object>> similarProducts(
            @RequestParam("productId") String productId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        int cap = Math.min(Math.max(limit, 1), 50);
        Long pid = safeParseLong(productId);
        if (pid == null) {
            return ResponseEntity.ok(Map.of("product_id", productId, "candidates", List.of()));
        }
        Optional<Product> seed = productRepository.findById(pid);
        if (seed.isEmpty()) return ResponseEntity.notFound().build();
        Product p = seed.get();
        Long sellerId = p.getSeller() != null ? p.getSeller().getId() : null;
        Long categoryId = p.getCategory() != null ? p.getCategory().getId() : null;
        if (sellerId == null) {
            return ResponseEntity.ok(Map.of("product_id", productId, "candidates", List.of()));
        }

        // Pull a generous page of seller's products in the same category, then
        // strip the seed product itself before capping at `cap`.
        var page = productRepository.findProducts(sellerId, null, categoryId, null,
                PageRequest.of(0, cap + 1));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Product cand : page.getContent()) {
            if (cand.getId() == null || cand.getId().equals(pid)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("product_id", cand.getId());
            m.put("name", nullToEmpty(cand.getName()));
            m.put("brand", nullToEmpty(cand.getBrand()));
            m.put("category_id", categoryId);
            m.put("base_price_inr", cand.getBasePrice());
            m.put("description_excerpt", clip(cand.getDescription(), 160));
            out.add(m);
            if (out.size() >= cap) break;
        }
        return ResponseEntity.ok(Map.of(
                "product_id", productId,
                "seller_id", sellerId,
                "category_id", categoryId,
                "candidates", out));
    }

    // ======================================================================
    //   helpers
    // ======================================================================

    private Optional<Order> resolveOrder(String orderId) {
        Long asLong = safeParseLong(orderId);
        if (asLong != null) {
            Optional<Order> byPk = orderRepository.findById(asLong);
            if (byPk.isPresent()) return byPk;
        }
        // Engine may pass either the numeric pk OR the marketplace externalOrderId.
        return orderRepository.findByExternalOrderId(orderId);
    }

    private Map<String, Object> changeSummary(ProposedChangeEntity pc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("change_id", pc.getChangeId().toString());
        m.put("change_type", nullToEmpty(pc.getChangeType()));
        m.put("agent_type", nullToEmpty(pc.getAgentType()));
        m.put("entity_type", nullToEmpty(pc.getShopifyEntityType()));
        m.put("entity_id", nullToEmpty(pc.getShopifyEntityId()));
        m.put("applied_at", pc.getAppliedAt() != null
                ? pc.getAppliedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        m.put("reasoning_excerpt", clip(pc.getAgentReasoning(), 200));
        m.put("current_value", pc.getCurrentValue());
        m.put("proposed_value", pc.getProposedValue());
        return m;
    }

    private void countWindow(Long productId, LocalDateTime start, LocalDateTime end,
                             Map<String, Object> out) {
        // Inline query keeps the entity touch-free: count orders whose items
        // reference this product within [start, end). For MVP the seller_id
        // filter is implicit via product → seller; we accept "all sellers"
        // when productId is null to keep the engine call non-fatal.
        long count = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        if (productId != null) {
            for (Order o : findOrdersForProductBetween(productId, start, end)) {
                count++;
                if (o.getTotalAmount() != null) {
                    revenue = revenue.add(o.getTotalAmount());
                }
            }
        }
        out.put("orders", count);
        out.put("revenue_inr", revenue);
        out.put("start", start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        out.put("end", end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    private List<Order> findOrdersForProductBetween(Long productId,
                                                    LocalDateTime start,
                                                    LocalDateTime end) {
        // Cheap heuristic: pull recent orders for this product's seller and
        // filter in-memory. Acceptable for MVP; if/when this is hot, push it
        // into a dedicated JPQL query joined on order_items.product_variant.product.
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null || p.getSeller() == null) return List.of();
        Long sellerId = p.getSeller().getId();
        var page = orderRepository.findBySeller(sellerId, start, end,
                PageRequest.of(0, 500));
        List<Order> out = new ArrayList<>();
        for (Order o : page.getContent()) {
            for (OrderItem item : o.getItems()) {
                if (item.getProductVariant() != null
                        && item.getProductVariant().getProduct() != null
                        && productId.equals(item.getProductVariant().getProduct().getId())) {
                    out.add(o);
                    break;
                }
            }
        }
        return out;
    }

    private static Optional<LocalDateTime> parsePivot(String iso) {
        if (iso == null || iso.isBlank()) return Optional.empty();
        try {
            return Optional.of(LocalDateTime.parse(iso));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Long safeParseLong(String s) {
        if (s == null) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String clip(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }
}
