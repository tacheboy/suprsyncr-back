package com.suprsyncr.autopilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.analytics.dto.ProductHealthResponse;
import com.suprsyncr.analytics.dto.SeoGapResponse;
import com.suprsyncr.analytics.dto.RevenueLeakResponse;
import com.suprsyncr.autopilot.domain.ProposedChangeEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Generates realistic stub proposals directly from analytics data when the
 * Python Agent Service is unavailable.
 *
 * This enables the full autopilot flow (run â†’ queue â†’ approve â†’ apply)
 * to work end-to-end without requiring the LangGraph/Gemini pipeline.
 *
 * Each stub proposal uses real product data from the store's analytics
 * snapshots with template-based reasoning and proposed values.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StubProposalGenerator {

    private final ObjectMapper objectMapper;

    private static final int MAX_PROPOSALS_PER_AGENT = 3;

    /**
     * Generate stub proposals for the specified agents using analytics context.
     *
     * @param storeId  Store identifier
     * @param runId    Run UUID
     * @param context  Pre-built analytics context from the orchestrator
     * @param agents   Specific agents to generate for (null = all)
     * @return List of stub proposals ready for persistence
     */
    public List<ProposedChangeEntity> generateStubProposals(
            String storeId, UUID runId, Map<String, Object> context, List<String> agents) {

        List<ProposedChangeEntity> proposals = new ArrayList<>();
        Set<String> agentSet = agents != null
                ? new HashSet<>(agents.stream().map(String::toLowerCase).toList())
                : Set.of("seo", "listing", "pricing", "cart_recovery");

        // Extract analytics data from context
        Map<String, Object> productHealthMap = safeMap(context.get("product_health"));
        Map<String, Object> seoGapsMap = safeMap(context.get("seo_gaps"));
        Map<String, Object> revenueLeakMap = safeMap(context.get("revenue_leak"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = productHealthMap.containsKey("products")
                ? (List<Map<String, Object>>) productHealthMap.get("products")
                : List.of();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> seoOpportunities = seoGapsMap.containsKey("opportunities")
                ? (List<Map<String, Object>>) seoGapsMap.get("opportunities")
                : List.of();

        // â”€â”€â”€ SEO Commander â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (agentSet.contains("seo")) {
            int count = 0;
            for (Map<String, Object> opp : seoOpportunities) {
                if (count >= MAX_PROPOSALS_PER_AGENT) break;
                String query = String.valueOf(opp.getOrDefault("query", "target keyword"));
                String productId = String.valueOf(opp.getOrDefault("productId", "unknown"));
                String currentTitle = String.valueOf(opp.getOrDefault("currentTitle", "Product Title"));

                // Generate META_TITLE proposal
                proposals.add(buildProposal(storeId, runId, "SEO_COMMANDER", "META_TITLE",
                        productId, currentTitle,
                        injectKeyword(currentTitle, query),
                        String.format(
                            "Diagnosis: Product ranks position %s for \"%s\" â€” within striking distance of page 1. "
                            + "Evidence: Google Search Console shows %s impressions but low CTR. "
                            + "Hypothesis: Keyword-optimized meta title will improve organic CTR by 15-30%%. "
                            + "Fix: Inject primary keyword \"%s\" into meta title while maintaining readability.",
                            opp.getOrDefault("position", "12"), query,
                            opp.getOrDefault("impressions", "500"), query),
                        "organic_traffic", 25.0, 8500, 0.72, "LOW",
                        "Keyword insertion preserves brand voice. Reversible if CTR drops."));
                count++;

                // Generate META_DESCRIPTION proposal
                if (count < MAX_PROPOSALS_PER_AGENT) {
                    String currentDesc = String.valueOf(opp.getOrDefault("currentDescription",
                            "Shop the best products online with free shipping."));
                    proposals.add(buildProposal(storeId, runId, "SEO_COMMANDER", "META_DESCRIPTION",
                            productId, currentDesc,
                            String.format("Shop %s online â€” %s. Free shipping, easy returns & genuine quality guaranteed.",
                                    query, currentTitle),
                            String.format(
                                "Diagnosis: Meta description lacks target keyword \"%s\" and has no compelling CTA. "
                                + "Evidence: Current organic CTR is below category average. "
                                + "Hypothesis: Keyword-rich description with trust signals will lift CTR 10-20%%. "
                                + "Fix: Rewrite with keyword, value prop, and urgency signals.",
                                query),
                            "organic_traffic", 18.0, 5200, 0.65, "LOW",
                            "Meta description changes are cosmetic and easily reversible."));
                    count++;
                }
            }
        }

        // â”€â”€â”€ Listing Doctor â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (agentSet.contains("listing")) {
            int count = 0;
            for (Map<String, Object> product : products) {
                if (count >= MAX_PROPOSALS_PER_AGENT) break;
                String quadrant = String.valueOf(product.getOrDefault("quadrant", ""));
                if (!"LISTING_PROBLEM".equals(quadrant)) continue;

                String productId = String.valueOf(product.getOrDefault("productId", "unknown"));
                String name = String.valueOf(product.getOrDefault("name", "Product"));
                double cvr = toDouble(product.get("conversionRate"), 1.5);
                int views = toInt(product.get("pageViews"), 500);

                // PRODUCT_TITLE proposal
                proposals.add(buildProposal(storeId, runId, "LISTING_DOCTOR", "PRODUCT_TITLE",
                        productId,
                        name,
                        enhanceTitle(name),
                        String.format(
                            "Diagnosis: \"%s\" has %d weekly page views but only %.1f%% CVR â€” classic listing problem. "
                            + "Evidence: High traffic + low conversion indicates the title fails to communicate value. "
                            + "Hypothesis: Adding material, benefit, and audience cues to the title will lift CVR 10-25%%. "
                            + "Fix: Restructure title with [Material] [Product] for [Audience] | [USP] pattern.",
                            name, views, cvr),
                        "conversion_rate", 18.0, 12000, 0.70, "LOW",
                        "Title change is cosmetic. Original preserved for rollback."));
                count++;

                // PRODUCT_DESCRIPTION proposal
                if (count < MAX_PROPOSALS_PER_AGENT) {
                    proposals.add(buildProposal(storeId, runId, "LISTING_DOCTOR", "PRODUCT_DESCRIPTION",
                            productId,
                            "Current product description lacks structure and trust signals.",
                            String.format(
                                "<h3>Why Choose %s?</h3>\n"
                                + "<ul>\n"
                                + "  <li>âœ… Premium quality â€” crafted for lasting value</li>\n"
                                + "  <li>ðŸ“¦ Free express shipping on all orders</li>\n"
                                + "  <li>ðŸ”„ 30-day hassle-free returns</li>\n"
                                + "  <li>â­ Trusted by 1,000+ happy customers</li>\n"
                                + "</ul>\n"
                                + "<p>Experience the difference â€” order today with confidence.</p>",
                                name),
                            String.format(
                                "Diagnosis: \"%s\" product description is plain text with no formatting. "
                                + "Evidence: Category benchmarks show top converters use bullet points, trust signals, and structured HTML. "
                                + "Hypothesis: Adding structured description with benefit-led bullets will lift CVR 8-20%%. "
                                + "Fix: Rewrite with benefit bullets, trust signals, and clear CTAs.",
                                name),
                            "conversion_rate", 14.0, 9500, 0.65, "MEDIUM",
                            "Description rewrite is subjective. Review tone and accuracy before applying."));
                    count++;
                }
            }
        }

        // â”€â”€â”€ Pricing Strategist â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (agentSet.contains("pricing")) {
            int count = 0;
            for (Map<String, Object> product : products) {
                if (count >= MAX_PROPOSALS_PER_AGENT) break;
                String quadrant = String.valueOf(product.getOrDefault("quadrant", ""));
                if (!"WINNER".equals(quadrant)) continue;

                String productId = String.valueOf(product.getOrDefault("productId", "unknown"));
                String name = String.valueOf(product.getOrDefault("name", "Product"));
                double price = toDouble(product.get("price"), 999);

                double testPrice = Math.round(price * 1.08);

                proposals.add(buildProposal(storeId, runId, "PRICING_STRATEGIST", "PRICE_CHANGE",
                        productId,
                        String.format("%.0f", price),
                        String.format("%.0f", testPrice),
                        String.format(
                            "Diagnosis: \"%s\" is a WINNER product (high traffic + high conversion) priced at â‚¹%.0f. "
                            + "Evidence: Category median price is higher â€” room for a price ceiling test. "
                            + "Hypothesis: 8%% price increase (â‚¹%.0f â†’ â‚¹%.0f) will be absorbed without CVR drop, lifting AOV. "
                            + "Fix: A/B test the higher price for 7 days with automatic revert if CVR drops >5%%.",
                            name, price, price, testPrice),
                        "revenue", 8.0, 15000, 0.55, "HIGH",
                        "Price increase may reduce conversion. Automatic 7-day revert if CVR drops >5%. Monitor closely."));
                count++;
            }
        }

        // â”€â”€â”€ Cart Recovery â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (agentSet.contains("cart_recovery")) {
            Double abandonmentRate = toDoubleObj(revenueLeakMap.get("overallAbandonmentRate"));
            Double totalLeak = toDoubleObj(revenueLeakMap.get("totalLeakINR"));

            if (abandonmentRate != null && abandonmentRate > 0.3) {
                // Find products with high abandonment
                int count = 0;
                for (Map<String, Object> product : products) {
                    if (count >= MAX_PROPOSALS_PER_AGENT) break;
                    double prodAbandonment = toDouble(product.get("abandonmentRate"), 0);
                    if (prodAbandonment < 0.5) continue;

                    String productId = String.valueOf(product.getOrDefault("productId", "unknown"));
                    String name = String.valueOf(product.getOrDefault("name", "Product"));

                    proposals.add(buildProposal(storeId, runId, "CART_RECOVERY", "PRODUCT_DESCRIPTION",
                            productId,
                            "Product page lacks trust signals for hesitant buyers.",
                            String.format(
                                "<div class=\"trust-signals\">\n"
                                + "  <p>ðŸ›¡ï¸ <strong>100%% Genuine Product</strong> â€” Verified & quality-checked</p>\n"
                                + "  <p>ðŸšš <strong>Free Shipping</strong> â€” No hidden charges at checkout</p>\n"
                                + "  <p>ðŸ”„ <strong>Easy Returns</strong> â€” 15-day no-questions-asked return policy</p>\n"
                                + "  <p>ðŸ’³ <strong>Secure Payment</strong> â€” All major cards & UPI accepted</p>\n"
                                + "</div>"),
                            String.format(
                                "Diagnosis: \"%s\" has %.0f%% cart abandonment â€” well above the %.0f%% store average. "
                                + "Evidence: Total revenue leak is â‚¹%s/month from abandoned carts. "
                                + "Hypothesis: Adding trust signals (shipping, returns, security) will reduce abandonment 5-15%%. "
                                + "Fix: Inject trust signal block into product description.",
                                name, prodAbandonment * 100, (abandonmentRate != null ? abandonmentRate * 100 : 70),
                                totalLeak != null ? String.format("%.0f", totalLeak) : "N/A"),
                            "cart_abandonment", -12.0, 8000, 0.60, "LOW",
                            "Trust signal injection is additive â€” does not remove existing content."));
                    count++;
                }
            }
        }

        // If no analytics data produced any proposals, generate universal stubs
        if (proposals.isEmpty()) {
            proposals.add(buildProposal(storeId, runId, "SEO_COMMANDER", "META_TITLE",
                    "sample-product-1",
                    "My Store Product",
                    "Premium Quality Product â€” Shop Online | Free Shipping & Returns",
                    "Diagnosis: Meta title is generic and lacks keywords. "
                    + "Evidence: No primary keywords in title tag. "
                    + "Hypothesis: Keyword-rich title will improve organic CTR by 20-35%. "
                    + "Fix: Add primary keyword, brand differentiator, and trust signal.",
                    "organic_traffic", 28.0, 7500, 0.68, "LOW",
                    "Meta title change is purely cosmetic and fully reversible."));

            proposals.add(buildProposal(storeId, runId, "LISTING_DOCTOR", "PRODUCT_DESCRIPTION",
                    "sample-product-2",
                    "Basic product description without formatting.",
                    "<h3>Why You'll Love This Product</h3>\n"
                    + "<ul>\n"
                    + "  <li>âœ… Premium materials â€” built to last</li>\n"
                    + "  <li>ðŸ“¦ Free express shipping within India</li>\n"
                    + "  <li>ðŸ”„ 30-day easy returns</li>\n"
                    + "  <li>â­ 4.5â˜… average rating from verified buyers</li>\n"
                    + "</ul>",
                    "Diagnosis: Product description is plain text without structure. "
                    + "Evidence: Top-performing competitors use bullet points and trust signals. "
                    + "Hypothesis: Structured description will lift CVR by 12-18%. "
                    + "Fix: Rewrite with benefit-led bullets and social proof.",
                    "conversion_rate", 15.0, 10000, 0.62, "MEDIUM",
                    "Description rewrite may need tone review for brand consistency."));
        }

        log.info("Generated {} stub proposals for store {} (run {})", proposals.size(), storeId, runId);
        return proposals;
    }

    /**
     * Compute estimated total impact from a list of proposals.
     */
    public BigDecimal computeTotalImpact(List<ProposedChangeEntity> proposals) {
        BigDecimal total = BigDecimal.ZERO;
        for (ProposedChangeEntity p : proposals) {
            try {
                if (p.getEstimatedImpact() != null) {
                    var impactNode = objectMapper.readTree(p.getEstimatedImpact());
                    double lift = impactNode.path("revenue_lift_inr").asDouble(0);
                    total = total.add(BigDecimal.valueOf(lift));
                }
            } catch (Exception ignored) {}
        }
        return total;
    }

    // â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private ProposedChangeEntity buildProposal(
            String storeId, UUID runId, String agentType, String changeType,
            String entityId, String currentValue, String proposedValue,
            String reasoning, String impactMetric, double changePercent,
            double revenueLiftInr, double confidence, String riskLevel,
            String riskNotes) {
        try {
            String impactJson = objectMapper.writeValueAsString(Map.of(
                    "metric", impactMetric,
                    "change_percent", changePercent,
                    "revenue_lift_inr", revenueLiftInr,
                    "confidence", confidence
            ));

            return ProposedChangeEntity.builder()
                    .changeId(UUID.randomUUID())
                    .storeId(storeId)
                    .runId(runId)
                    .agentType(agentType)
                    .changeType(changeType)
                    .shopifyEntityType("product")
                    .shopifyEntityId(entityId)
                    .currentValue(objectMapper.writeValueAsString(currentValue))
                    .proposedValue(objectMapper.writeValueAsString(proposedValue))
                    .agentReasoning(reasoning)
                    .estimatedImpact(impactJson)
                    .riskLevel(riskLevel)
                    .riskNotes(riskNotes)
                    .isTest("PRICE_CHANGE".equals(changeType))
                    .build();
        } catch (Exception e) {
            log.error("Failed to build stub proposal: {}", e.getMessage());
            throw new RuntimeException("Stub proposal construction failed", e);
        }
    }

    private String injectKeyword(String title, String keyword) {
        // Simple keyword injection: prepend if not already present
        if (title.toLowerCase().contains(keyword.toLowerCase())) {
            return title;
        }
        return keyword.substring(0, 1).toUpperCase() + keyword.substring(1) + " â€” " + title;
    }

    private String enhanceTitle(String title) {
        // Add structured elements if missing
        if (title.contains("|") || title.contains("â€”")) return title;
        return title + " â€” Premium Quality | Free Shipping";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object obj) {
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return Map.of();
    }

    private double toDouble(Object obj, double fallback) {
        if (obj == null) return fallback;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (Exception e) { return fallback; }
    }

    private int toInt(Object obj, int fallback) {
        if (obj == null) return fallback;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return fallback; }
    }

    private Double toDoubleObj(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (Exception e) { return null; }
    }
}

