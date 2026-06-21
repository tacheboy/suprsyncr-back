package com.suprsyncr.autopilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.analytics.dto.ProductHealthResponse;
import com.suprsyncr.analytics.dto.RevenueLeakResponse;
import com.suprsyncr.analytics.dto.SeoGapResponse;
import com.suprsyncr.analytics.service.AnalyticsOrchestrator;
import com.suprsyncr.autopilot.domain.AgentRun;
import com.suprsyncr.autopilot.domain.ProposedChangeEntity;
import com.suprsyncr.autopilot.repository.AgentRunRepository;
import com.suprsyncr.product.entity.Product;
import com.suprsyncr.product.repository.ProductRepository;
import com.suprsyncr.seller.entity.SellerPlatform;
import com.suprsyncr.seller.repository.SellerPlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spring Boot-side lifecycle manager for agent runs.
 *
 * Responsibilities:
 * 1. Create AgentRun record (QUEUED)
 * 2. Gather analytics context from AnalyticsOrchestrator
 * 3. Run the inference engine with context
 * 4. Track run status (RUNNING â†’ COMPLETE / FAILED)
 * 5. Proposals are persisted via the /api/autopilot/proposals/batch endpoint
 *    (called by the Python service when run completes)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentRunOrchestratorService {

    private final AnalyticsOrchestrator analyticsOrchestrator;
    private final AgentRunRepository agentRunRepository;
    private final ChangeManagementService changeManagementService;
    private final StubProposalGenerator stubProposalGenerator;
    private final EngineRunService engineRunService;
    private final ProductRepository productRepository;
    private final SellerPlatformRepository platformRepository;
    private final ObjectMapper objectMapper;

    /** When true, runs go through the Python Inference Engine; stub is fallback only. */
    @Value("${autopilot.engine.enabled:true}")
    private boolean engineEnabled;

    /**
     * Trigger a full agent run for a store.
     *
     * @param storeId     Store to run agents for
     * @param triggeredBy SCHEDULED, MANUAL, or WEBHOOK
     * @return The created AgentRun entity
     */
    public AgentRun startRun(String storeId, String triggeredBy) {
        AgentRun run = AgentRun.builder()
                .runId(UUID.randomUUID())
                .storeId(storeId)
                .triggeredBy(triggeredBy)
                .triggeredAt(LocalDateTime.now())
                .status("QUEUED")
                .runType("FULL_PIPELINE")
                .build();
        log.info("Created agent run {} for store {} (triggered by {})", run.getRunId(), storeId, triggeredBy);
        return agentRunRepository.save(run);
    }

    /**
     * Trigger an individual service run with specific agents and optional product overrides.
     */
    public AgentRun startIndividualRun(String storeId, List<String> agents, List<String> productIds) {
        AgentRun run = AgentRun.builder()
                .runId(UUID.randomUUID())
                .storeId(storeId)
                .triggeredBy("MANUAL")
                .triggeredAt(LocalDateTime.now())
                .status("QUEUED")
                .runType("INDIVIDUAL")
                .selectedAgents(String.join(",", agents))
                .productOverrides(productIds != null && !productIds.isEmpty()
                        ? "[\"" + String.join("\",\"", productIds) + "\"]" : null)
                .build();
        log.info("Created INDIVIDUAL agent run {} for store {} (agents: {})", 
                run.getRunId(), storeId, agents);
        return agentRunRepository.save(run);
    }

    @Async
    public void executeRunAsync(AgentRun run) {
        try {
            run.setStatus("RUNNING");
            agentRunRepository.save(run);
            
            Map<String, Object> context = buildAnalyticsContext(run.getStoreId());
            
            // Determine if this is an individual or full run
            List<String> agents = null;
            List<String> productOverrides = null;
            if ("INDIVIDUAL".equals(run.getRunType()) && run.getSelectedAgents() != null) {
                agents = List.of(run.getSelectedAgents().split(","));
                if (run.getProductOverrides() != null) {
                    try {
                        productOverrides = objectMapper.readValue(
                                run.getProductOverrides(), 
                                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                    } catch (Exception e) {
                        log.warn("Failed to parse product overrides: {}", e.getMessage());
                    }
                }
            }
            
            // Primary path: the Python Inference Engine (supervisor → router → agents),
            // which returns grounded proposals + full cost/telemetry that we persist.
            boolean engineOk = false;
            if (engineEnabled) {
                engineOk = engineRunService.runAndPersist(run, context);
            }
            if (!engineOk) {
                log.warn("Inference Engine unavailable/failed â€” falling back to stub proposals for run {}", run.getRunId());
                generateAndPersistStubProposals(run, context, agents);
            }
        } catch (Exception e) {
            log.warn("Python Agent Service communication failed: {} â€” falling back to stub proposals", e.getMessage());
            try {
                Map<String, Object> fallbackContext = buildAnalyticsContext(run.getStoreId());
                List<String> fallbackAgents = null;
                if ("INDIVIDUAL".equals(run.getRunType()) && run.getSelectedAgents() != null) {
                    fallbackAgents = List.of(run.getSelectedAgents().split(","));
                }
                generateAndPersistStubProposals(run, fallbackContext, fallbackAgents);
            } catch (Exception fallbackError) {
                log.error("Stub proposal generation also failed for run {}: {}", run.getRunId(), fallbackError.getMessage());
                run.setStatus("FAILED");
                run.setErrorMessage("Both Python service and stub generation failed: " + fallbackError.getMessage());
                run.setCompletedAt(LocalDateTime.now());
                agentRunRepository.save(run);
            }
        }
    }

    /**
     * Generate stub proposals from analytics context and persist them.
     * Called as fallback when the Python Agent Service is unavailable.
     */
    private void generateAndPersistStubProposals(AgentRun run, Map<String, Object> context, List<String> agents) {
        List<ProposedChangeEntity> stubs = stubProposalGenerator.generateStubProposals(
                run.getStoreId(), run.getRunId(), context, agents);

        List<ProposedChangeEntity> saved = changeManagementService.proposeBatch(stubs);
        BigDecimal totalImpact = stubProposalGenerator.computeTotalImpact(saved);

        run.setStatus("COMPLETE");
        run.setProposalsGenerated(saved.size());
        run.setEstimatedImpactInr(totalImpact);
        run.setCompletedAt(LocalDateTime.now());
        agentRunRepository.save(run);

        log.info("Stub proposals generated for run {}: {} proposals, â‚¹{} estimated impact",
                run.getRunId(), saved.size(), totalImpact);
    }

    /**
     * Build the full analytics context payload from pre-computed snapshots.
     * This is the "perception layer" that agents see.
     *
     * <p>For a freshly-connected real store there are no orders, no funnel data
     * and no analytics snapshot — handing that empty context to the inference
     * engine would yield zero proposals and an empty Approval Queue. While the
     * real Shopify sync is still being built out, we fall back to a known good
     * demo store ({@code DEMO_FALLBACK_STORE_ID}) for any storeId that hasn't
     * accumulated its own data yet. Proposals are still persisted under the
     * caller's actual storeId, so the user sees them in their queue.
     */
    private static final String DEMO_FALLBACK_STORE_ID = "store-a";

    private Map<String, Object> buildAnalyticsContext(String storeId) {
        // 1. Real analytics snapshot for this storeId (preferred path).
        Map<String, Object> context = buildAnalyticsContextFor(storeId);
        if (isContextUsable(context)) return context;

        // 2. Local catalogue evidence: if a numeric storeId maps to a SellerPlatform
        //    whose products have been synced, build an evidence pack from those real
        //    products. The agent then reasons over the seller's actual catalogue —
        //    proposals reference real Shopify entity ids, so apply hits real Shopify.
        Map<String, Object> localCtx = buildEvidenceFromLocalCatalogue(storeId);
        if (isContextUsable(localCtx)) {
            log.info("Analytics snapshot empty for store {} — using local synced catalogue as evidence", storeId);
            return localCtx;
        }

        // 3. Last resort: demo store. The seller sees the engine running, but
        //    the resulting proposals are isTest (entity ids don't exist on their
        //    real storefront), so apply will simulate.
        if (!DEMO_FALLBACK_STORE_ID.equals(storeId)) {
            log.info("No local catalogue for store {} either — bootstrapping evidence pack from {}",
                    storeId, DEMO_FALLBACK_STORE_ID);
            return buildAnalyticsContextFor(DEMO_FALLBACK_STORE_ID);
        }
        return context;
    }

    /**
     * Synthesise a product_health evidence section from the seller's locally-
     * synced Shopify products. Funnel signals (traffic, cvr, abandonment) require
     * order history we don't have yet, so we mark them with placeholder values
     * that flag every product as a LISTING_PROBLEM candidate — the legitimate
     * conservative guess for a freshly-synced catalogue where nothing has been
     * optimised yet. When orders sync lands these placeholders go away.
     */
    private Map<String, Object> buildEvidenceFromLocalCatalogue(String storeId) {
        Long platformId;
        try { platformId = Long.parseLong(storeId.trim()); }
        catch (Exception e) { return Map.of(); }

        SellerPlatform platform = platformRepository.findById(platformId).orElse(null);
        if (platform == null || platform.getSeller() == null) return Map.of();

        // Page through up to 50 products — enough for the planner to triage.
        List<Product> products = productRepository.findProducts(
                platform.getSeller().getId(), null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
        if (products.isEmpty()) return Map.of();

        List<Map<String, Object>> phRows = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> row = new HashMap<>();
            // Use the Shopify product id (from the sku we stored: "shopify-<id>") so
            // the agent's proposals target the real Shopify entity.
            String externalId = p.getSku() != null && p.getSku().startsWith("shopify-")
                    ? p.getSku().substring("shopify-".length())
                    : String.valueOf(p.getId());
            row.put("productId", externalId);
            row.put("name", p.getName());
            row.put("description", p.getDescription() == null ? "" : p.getDescription());
            row.put("category", p.getCategory() != null ? p.getCategory().getName() : "Uncategorised");
            row.put("price", p.getBasePrice() != null ? p.getBasePrice() : BigDecimal.ZERO);
            row.put("aov", p.getBasePrice() != null ? p.getBasePrice() : BigDecimal.ZERO);
            // Placeholder funnel signals — flag as LISTING_PROBLEM so the agent
            // considers a rewrite. Replaced with real funnel once orders sync.
            row.put("traffic", 400);
            row.put("pageViews", 400);
            row.put("cvr", 1.2);
            row.put("conversionRate", 1.2);
            row.put("quadrant", "LISTING_PROBLEM");
            phRows.add(row);
        }

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("product_health", Map.of(
                "storeId", storeId,
                "dataSource", "live_shopify_catalogue",
                "products", phRows));
        ctx.put("revenue_leak", Map.of());
        ctx.put("seo_gaps", Map.of());
        return ctx;
    }

    /**
     * Heuristic: a context is "usable" if it has at least one product-health row
     * to reason over. Empty/missing sections are tolerated, but a completely
     * empty product list means the agents have nothing to act on.
     */
    @SuppressWarnings("unchecked")
    private boolean isContextUsable(Map<String, Object> context) {
        Object ph = context.get("product_health");
        if (!(ph instanceof Map)) return false;
        Object products = ((Map<String, Object>) ph).get("products");
        return products instanceof java.util.Collection && !((java.util.Collection<?>) products).isEmpty();
    }

    private Map<String, Object> buildAnalyticsContextFor(String storeId) {
        Map<String, Object> context = new HashMap<>();

        try {
            RevenueLeakResponse revenueLeak = analyticsOrchestrator.getRevenueLeak(storeId);
            context.put("revenue_leak", objectMapper.convertValue(revenueLeak, Map.class));
        } catch (Exception e) {
            log.warn("Failed to get revenue leak for context: {}", e.getMessage());
            context.put("revenue_leak", Map.of());
        }

        try {
            ProductHealthResponse productHealth = analyticsOrchestrator.getProductHealth(storeId);
            context.put("product_health", objectMapper.convertValue(productHealth, Map.class));
        } catch (Exception e) {
            log.warn("Failed to get product health for context: {}", e.getMessage());
            context.put("product_health", Map.of());
        }

        try {
            SeoGapResponse seoGaps = analyticsOrchestrator.getSeoGaps(storeId);
            context.put("seo_gaps", objectMapper.convertValue(seoGaps, Map.class));
        } catch (Exception e) {
            log.warn("Failed to get SEO gaps for context: {}", e.getMessage());
            context.put("seo_gaps", Map.of());
        }

        log.info("Built analytics context for store {} with {} sections", storeId, context.size());
        return context;
    }

    /**
     * Get all runs for a store, ordered by most recent first.
     */
    public List<AgentRun> getRunsForStore(String storeId) {
        return agentRunRepository.findByStoreIdOrderByTriggeredAtDesc(storeId);
    }

    /**
     * Get a specific run by ID.
     */
    public AgentRun getRun(UUID runId) {
        return agentRunRepository.findById(runId).orElse(null);
    }

    /**
     * Mark a run as complete (called when proposals are ingested from Python service).
     */
    public void markRunComplete(UUID runId, int proposalCount, BigDecimal estimatedImpact) {
        agentRunRepository.findById(runId).ifPresent(run -> {
            run.setStatus("COMPLETE");
            run.setProposalsGenerated(proposalCount);
            run.setEstimatedImpactInr(estimatedImpact);
            run.setCompletedAt(LocalDateTime.now());
            agentRunRepository.save(run);
            log.info("Agent run {} marked COMPLETE: {} proposals, â‚¹{} estimated impact",
                    runId, proposalCount, estimatedImpact);
        });
    }

    /**
     * Build preview data for individual services cards.
     * Returns mini analytics summaries from the latest computed snapshots.
     */
    public Map<String, Object> getServicesPreview(String storeId) {
        Map<String, Object> preview = new HashMap<>();

        try {
            ProductHealthResponse health = analyticsOrchestrator.getProductHealth(storeId);
            if (health != null && health.getProducts() != null) {
                long listingProblems = health.getProducts().stream()
                        .filter(p -> "LISTING_PROBLEM".equals(p.getQuadrant()))
                        .count();
                long winners = health.getProducts().stream()
                        .filter(p -> "WINNER".equals(p.getQuadrant()))
                        .count();
                String topProduct = health.getProducts().stream()
                        .filter(p -> "LISTING_PROBLEM".equals(p.getQuadrant()))
                        .findFirst()
                        .map(p -> p.getName() != null ? p.getName() : "")
                        .orElse("");
                
                preview.put("listing", Map.of(
                        "problemProductCount", listingProblems,
                        "topProductName", topProduct,
                        "available", true
                ));
                preview.put("pricing", Map.of(
                        "winnerCount", winners,
                        "available", true
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to build listing/pricing preview: {}", e.getMessage());
            preview.put("listing", Map.of("available", false));
            preview.put("pricing", Map.of("available", false));
        }

        try {
            SeoGapResponse seoGaps = analyticsOrchestrator.getSeoGaps(storeId);
            if (seoGaps != null && seoGaps.getOpportunities() != null) {
                int count = seoGaps.getOpportunities().size();
                String topKeyword = seoGaps.getOpportunities().stream()
                        .findFirst()
                        .map(o -> o.getQuery() != null ? o.getQuery() : "")
                        .orElse("");
                preview.put("seo", Map.of(
                        "opportunityCount", count,
                        "topKeyword", topKeyword,
                        "available", true
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to build SEO preview: {}", e.getMessage());
            preview.put("seo", Map.of("available", false));
        }

        try {
            RevenueLeakResponse revLeak = analyticsOrchestrator.getRevenueLeak(storeId);
            if (revLeak != null) {
                preview.put("cart_recovery", Map.of(
                        "totalLeakINR", revLeak.getTotalLeakINR() != null ? revLeak.getTotalLeakINR() : 0,
                        "abandonmentRate", revLeak.getOverallAbandonmentRate() != null ? revLeak.getOverallAbandonmentRate() : 0,
                        "available", true
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to build cart recovery preview: {}", e.getMessage());
            preview.put("cart_recovery", Map.of("available", false));
        }

        preview.put("competitor_intel", Map.of("available", true));

        return preview;
    }
}

