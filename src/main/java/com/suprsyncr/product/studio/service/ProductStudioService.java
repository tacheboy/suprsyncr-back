package com.suprsyncr.product.studio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.autopilot.domain.ProposedChangeEntity;
import com.suprsyncr.autopilot.repository.ProposedChangeRepository;
import com.suprsyncr.product.entity.Product;
import com.suprsyncr.product.entity.ProductStatus;
import com.suprsyncr.product.repository.ProductRepository;
import com.suprsyncr.product.studio.dto.CreateDraftRequest;
import com.suprsyncr.product.studio.dto.DraftDto;
import com.suprsyncr.product.studio.dto.PublishDraftRequest;
import com.suprsyncr.product.studio.entity.ProductDraft;
import com.suprsyncr.product.studio.entity.ProductDraftStatus;
import com.suprsyncr.product.studio.internal.StoreContextResolver;
import com.suprsyncr.product.studio.repository.ProductDraftRepository;
import com.suprsyncr.integration.connector.ShopifyConnector;
import com.suprsyncr.integration.shopify.ShopifyCredentialResolver;
import com.suprsyncr.seller.entity.Seller;
import com.suprsyncr.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the Studio lifecycle:
 *   create → engine call → persist 3 columns → seller edits → publish.
 *
 * The engine call is synchronous on purpose for MVP — the typical run is 4–8s
 * and the frontend can wait. We keep the draft in PENDING_AI for the duration
 * of the call so a refresh during the call shows the right state; on
 * completion (or error) we update once.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStudioService {

    private final ProductDraftRepository draftRepository;
    private final EngineStudioClient engineClient;
    private final StoreContextResolver storeContextResolver;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final ProposedChangeRepository proposedChangeRepository;
    private final ShopifyConnector shopifyConnector;
    private final ShopifyCredentialResolver shopifyCredentialResolver;
    private final ObjectMapper objectMapper;

    @Value("${studio.publish-to-shopify:false}")
    private boolean publishToShopify;

    // ---- create ---------------------------------------------------------------

    /**
     * Persist a PENDING_AI draft, call the engine, persist its three columns.
     * One DB row is touched twice: once to create, once after the engine
     * returns. We deliberately do not wrap engine I/O in a transaction —
     * the engine call may take seconds and we don't want to hold a DB
     * connection for the duration.
     */
    public DraftDto createDraft(CreateDraftRequest req) {
        ProductDraft draft = persistInitial(req);
        try {
            JsonNode engineResp = engineClient.runStudio(
                    draft.getDraftId(),
                    draft.getStoreId(),
                    draft.getImageUrl(),
                    draft.getClaimedTitle(),
                    draft.getPosture(),
                    req.getServices());
            persistEngineResult(draft.getDraftId(), engineResp);
        } catch (Exception e) {
            log.warn("studio engine call failed for draft {}: {}",
                    draft.getDraftId(), e.getMessage());
            markFailed(draft.getDraftId(), e.getMessage());
        }
        ProductDraft refreshed = draftRepository.findById(draft.getDraftId())
                .orElseThrow(() -> new IllegalStateException(
                        "draft vanished after engine call: " + draft.getDraftId()));
        return DraftDto.from(refreshed, objectMapper);
    }

    @Transactional
    protected ProductDraft persistInitial(CreateDraftRequest req) {
        ProductDraft draft = ProductDraft.builder()
                .storeId(req.getStoreId())
                .sellerId(storeContextResolver.resolveSellerId(req.getStoreId()).orElse(null))
                .status(ProductDraftStatus.PENDING_AI)
                .imageUrl(req.getImageUrl())
                .claimedTitle(req.getClaimedTitle())
                .posture(req.getPosture() == null ? "balanced" : req.getPosture())
                .build();
        return draftRepository.save(draft);
    }

    @Transactional
    protected void persistEngineResult(UUID draftId, JsonNode engineResp) {
        ProductDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalStateException("draft not found: " + draftId));

        String engineStatus = textOrNull(engineResp.path("status"));
        boolean usable = "COMPLETE".equals(engineStatus) || "PARTIAL".equals(engineStatus);
        draft.setStatus(usable ? ProductDraftStatus.AI_COMPLETE : ProductDraftStatus.FAILED);

        draft.setCopyColumn(writeNode(engineResp.path("copy_column")));
        draft.setSeoColumn(writeNode(engineResp.path("seo_column")));
        draft.setMetadataColumn(writeNode(engineResp.path("metadata_column")));
        draft.setIdentifiedProduct(writeNode(engineResp.path("identified")));
        draft.setMismatchWarning(writeNode(engineResp.path("mismatch")));
        draft.setModelPath(textOrNull(engineResp.path("model_path")));
        draft.setCostInr(decimalOrNull(engineResp.path("total_cost_inr")));
        draft.setConfidence(decimalOrNull(engineResp.path("confidence")));
        draft.setPlanReasoning(textOrNull(engineResp.path("plan_reasoning")));

        if (!usable) {
            draft.setErrorMessage(textOrNull(engineResp.path("error")));
        } else {
            draft.setAiCompletedAt(LocalDateTime.now());
        }
        draftRepository.save(draft);
    }

    @Transactional
    protected void markFailed(UUID draftId, String message) {
        draftRepository.findById(draftId).ifPresent(d -> {
            d.setStatus(ProductDraftStatus.FAILED);
            d.setErrorMessage(message);
            draftRepository.save(d);
        });
    }

    // ---- read ----------------------------------------------------------------

    public Optional<DraftDto> getDraft(UUID draftId) {
        return draftRepository.findById(draftId).map(d -> DraftDto.from(d, objectMapper));
    }

    public java.util.List<DraftDto> listDrafts(String storeId) {
        return draftRepository.findByStoreIdOrderByCreatedAtDesc(storeId).stream()
                .map(d -> DraftDto.from(d, objectMapper))
                .toList();
    }

    // ---- publish -------------------------------------------------------------

    /**
     * Apply seller edits, create the local Product, mark draft PUBLISHED.
     * Shopify push is feature-flagged; when off (MVP default) we still mark
     * PUBLISHED locally so the seller can see the listing in their catalogue.
     */
    @Transactional
    public DraftDto publishDraft(UUID draftId, PublishDraftRequest req) {
        ProductDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("draft not found: " + draftId));
        if (draft.getStatus() != ProductDraftStatus.AI_COMPLETE) {
            throw new IllegalStateException(
                    "draft is not in AI_COMPLETE state: " + draft.getStatus());
        }

        // Seller-edit overrides take precedence over engine output.
        if (req.getCopyColumn() != null)     draft.setCopyColumn(writeNode(req.getCopyColumn()));
        if (req.getSeoColumn() != null)      draft.setSeoColumn(writeNode(req.getSeoColumn()));
        if (req.getMetadataColumn() != null) draft.setMetadataColumn(writeNode(req.getMetadataColumn()));

        // Block publish on un-acknowledged mismatch unless seller overrides.
        JsonNode mismatch = readNode(draft.getMismatchWarning());
        if (mismatch != null && mismatch.path("mismatch").asBoolean(false)
                && !Boolean.TRUE.equals(req.getAcceptMismatchOverride())) {
            throw new IllegalStateException(
                    "mismatch_unconfirmed: identified="
                    + mismatch.path("identified").asText("")
                    + " seller_claim=" + mismatch.path("seller_claim").asText(""));
        }

        Long initialSellerId = draft.getSellerId();
        final Long sellerId = (initialSellerId != null) ? initialSellerId
                : storeContextResolver.resolveSellerId(draft.getStoreId()).orElseThrow(
                        () -> new IllegalStateException(
                                "no seller backing storeId: " + draft.getStoreId()));
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalStateException(
                        "seller not found: " + sellerId));

        Product product = buildProduct(draft, seller, req);
        Product saved = productRepository.save(product);

        draft.setPublishedProductId(saved.getId());
        draft.setStatus(ProductDraftStatus.PUBLISHED);
        draft.setPublishedAt(LocalDateTime.now());

        // Push to Shopify when enabled AND the store is a connected Shopify store.
        // The local catalogue product is already saved above, so a Shopify
        // failure leaves the seller with the product in our platform plus a
        // clear error — never a half-written listing.
        if (publishToShopify && shopifyCredentialResolver.isShopifyConnected(draft.getStoreId())) {
            try {
                Map<String, String> creds =
                        shopifyCredentialResolver.resolveCredentials(draft.getStoreId());
                String externalId = shopifyConnector.publishProduct(saved, creds);
                draft.setShopifyProductId(externalId);
                log.info("studio: published draft {} → Shopify product {} (store {})",
                        draftId, externalId, draft.getStoreId());
            } catch (Exception e) {
                // Local product stands; report the Shopify-side failure on the draft.
                draft.setErrorMessage("Listed locally, but Shopify push failed: "
                        + e.getMessage());
                log.warn("studio: Shopify push failed for draft {} (local product {} saved): {}",
                        draftId, saved.getId(), e.getMessage());
            }
        } else if (publishToShopify) {
            log.info("studio: publish-to-shopify on, but store {} is not a connected "
                     + "Shopify store — saved locally only", draft.getStoreId());
        }

        // Create an APPLIED ProposedChange so the attribution gate can find this
        // listing when an order arrives later.
        emitListingChange(draft, saved);

        draftRepository.save(draft);
        return DraftDto.from(draft, objectMapper);
    }

    private void emitListingChange(ProductDraft draft, Product product) {
        try {
            // Match the key format used by AgentRunOrchestratorService: for Shopify
            // products the entityId is the numeric part after "shopify-" in the sku.
            String shopifyEntityId = product.getSku() != null && product.getSku().startsWith("shopify-")
                    ? product.getSku().substring("shopify-".length())
                    : String.valueOf(product.getId());

            String proposedVal = objectMapper.writeValueAsString(Map.of(
                    "title", product.getName(),
                    "description", product.getDescription() != null ? product.getDescription() : "",
                    "sku", product.getSku() != null ? product.getSku() : ""
            ));

            ProposedChangeEntity change = ProposedChangeEntity.builder()
                    .storeId(draft.getStoreId())
                    .agentType("STUDIO")
                    .changeType("LISTING_CREATED")
                    .shopifyEntityType("product")
                    .shopifyEntityId(shopifyEntityId)
                    .currentValue("null")
                    .proposedValue(proposedVal)
                    .agentReasoning("Product Studio listing created via AI pipeline")
                    .riskLevel("LOW")
                    .status("APPLIED")
                    .approvedAt(LocalDateTime.now())
                    .appliedAt(LocalDateTime.now())
                    .costInr(draft.getCostInr())
                    .confidence(draft.getConfidence())
                    .modelPath(draft.getModelPath())
                    .build();

            proposedChangeRepository.save(change);
            log.info("studio: emitted LISTING_CREATED change for product {} (entityId={})",
                    product.getId(), shopifyEntityId);
        } catch (Exception e) {
            log.warn("studio: failed to emit listing change for draft {}: {}",
                    draft.getDraftId(), e.getMessage());
        }
    }

    private Product buildProduct(ProductDraft draft, Seller seller, PublishDraftRequest req) {
        JsonNode copy = readNode(draft.getCopyColumn());
        JsonNode meta = readNode(draft.getMetadataColumn());
        JsonNode identified = readNode(draft.getIdentifiedProduct());

        String name = optText(copy, "title")
                .or(() -> optText(identified, "title"))
                .orElse(draft.getClaimedTitle());
        String description = optText(copy, "description").orElse("");
        String brand = optText(meta, "vendor")
                .or(() -> optText(identified, "brand"))
                .orElse("");

        Product p = new Product();
        p.setSeller(seller);
        p.setName(name);
        p.setDescription(description);
        p.setBrand(brand);
        p.setSku(req.getSkuOverride() != null && !req.getSkuOverride().isBlank()
                ? req.getSkuOverride().trim()
                : generateSku(seller.getId(), draft.getDraftId()));
        p.setBasePrice(req.getBasePriceInr() != null ? req.getBasePriceInr() : BigDecimal.ZERO);
        p.setStatus(ProductStatus.ACTIVE);
        p.getImageUrls().add(draft.getImageUrl());
        return p;
    }

    private static String generateSku(Long sellerId, UUID draftId) {
        String suffix = draftId.toString().replace("-", "").substring(0, 8).toUpperCase();
        return "S" + sellerId + "-" + suffix;
    }

    // ---- JSON helpers --------------------------------------------------------

    private String writeNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode readNode(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static String textOrNull(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return null;
        if (n.isTextual()) return n.asText();
        return n.toString();
    }

    private static BigDecimal decimalOrNull(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull() || !n.isNumber()) return null;
        return BigDecimal.valueOf(n.asDouble());
    }

    private static Optional<String> optText(JsonNode node, String field) {
        if (node == null) return Optional.empty();
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) return Optional.empty();
        String s = v.asText("");
        return s.isBlank() ? Optional.empty() : Optional.of(s);
    }
}
