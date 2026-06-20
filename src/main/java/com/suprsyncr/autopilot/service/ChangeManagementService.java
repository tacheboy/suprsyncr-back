package com.suprsyncr.autopilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suprsyncr.autopilot.domain.EntitySnapshot;
import com.suprsyncr.autopilot.domain.ProposedChangeEntity;
import com.suprsyncr.autopilot.repository.EntitySnapshotRepository;
import com.suprsyncr.autopilot.repository.ProposedChangeRepository;
import com.suprsyncr.integration.connector.ShopifyConnector;
import com.suprsyncr.integration.shopify.ShopifyCredentialResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the lifecycle of proposed changes:
 *   PENDING_APPROVAL â†’ APPROVED â†’ APPLYING â†’ APPLIED
 *                    â†’ REJECTED
 *                                           â†’ APPLY_FAILED
 *                                  APPLIED  â†’ ROLLED_BACK
 *
 * Replaces the previous in-memory HashMap implementation with JPA persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeManagementService {

    private final ProposedChangeRepository changeRepository;
    private final EntitySnapshotRepository snapshotRepository;
    private final ShopifyConnector shopifyConnector;
    private final ShopifyCredentialResolver credentialResolver;
    private final ObjectMapper objectMapper;

    // â”€â”€â”€ Proposal Ingestion â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Persist a single proposed change (called when Python service sends proposals).
     */
    @Transactional
    public ProposedChangeEntity propose(ProposedChangeEntity change) {
        change.setStatus("PENDING_APPROVAL");
        ProposedChangeEntity saved = changeRepository.save(change);
        log.info("Proposed change {} stored (agent: {}, type: {})",
                saved.getChangeId(), saved.getAgentType(), saved.getChangeType());
        return saved;
    }

    /**
     * Persist a batch of proposals (called from the batch ingestion endpoint).
     */
    @Transactional
    public List<ProposedChangeEntity> proposeBatch(List<ProposedChangeEntity> changes) {
        changes.forEach(c -> c.setStatus("PENDING_APPROVAL"));
        List<ProposedChangeEntity> saved = changeRepository.saveAll(changes);
        log.info("Batch-persisted {} proposals", saved.size());
        return saved;
    }

    // â”€â”€â”€ Approval Queue â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Get all pending proposals for a store (the approval queue).
     */
    public List<ProposedChangeEntity> getPendingProposals(String storeId) {
        return changeRepository.findByStoreIdAndStatus(storeId, "PENDING_APPROVAL");
    }

    /**
     * Get all proposals for a store (including applied, rejected, etc.).
     */
    public List<ProposedChangeEntity> getAllProposals(String storeId) {
        return changeRepository.findByStoreIdOrderByChangeIdDesc(storeId);
    }

    /**
     * Get proposals for a specific run.
     */
    public List<ProposedChangeEntity> getProposalsByRun(UUID runId) {
        return changeRepository.findByRunId(runId);
    }

    // â”€â”€â”€ Approval â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Approve a single change. Does NOT apply it â€” the seller must explicitly trigger apply.
     */
    @Transactional
    public ProposedChangeEntity approve(UUID changeId, String approvedBy) {
        ProposedChangeEntity change = changeRepository.findById(changeId)
                .orElseThrow(() -> new IllegalArgumentException("Change not found: " + changeId));

        if (!"PENDING_APPROVAL".equals(change.getStatus())) {
            throw new IllegalStateException("Change is not pending approval: " + change.getStatus());
        }

        change.setStatus("APPROVED");
        change.setApprovedBy(approvedBy);
        change.setApprovedAt(LocalDateTime.now());
        changeRepository.save(change);

        log.info("Change {} approved by {}", changeId, approvedBy);
        return change;
    }

    /**
     * Reject a single change.
     */
    @Transactional
    public ProposedChangeEntity reject(UUID changeId) {
        ProposedChangeEntity change = changeRepository.findById(changeId)
                .orElseThrow(() -> new IllegalArgumentException("Change not found: " + changeId));

        change.setStatus("REJECTED");
        changeRepository.save(change);

        log.info("Change {} rejected", changeId);
        return change;
    }

    /**
     * Batch-approve all LOW risk changes for a store.
     */
    @Transactional
    public int batchApproveLowRisk(String storeId, String approvedBy) {
        List<ProposedChangeEntity> pending = getPendingProposals(storeId);
        int count = 0;
        for (ProposedChangeEntity change : pending) {
            if ("LOW".equals(change.getRiskLevel())) {
                change.setStatus("APPROVED");
                change.setApprovedBy(approvedBy);
                change.setApprovedAt(LocalDateTime.now());
                changeRepository.save(change);
                count++;
            }
        }
        log.info("Batch-approved {} LOW risk changes for store {}", count, storeId);
        return count;
    }

    // â”€â”€â”€ Apply (Shopify Write) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Apply an approved change to Shopify.
     * Credentials are loaded server-side from the store's stored OAuth token
     * (never supplied by the client). Snapshots the entity before writing so we
     * can rollback later.
     */
    @Transactional
    public boolean apply(UUID changeId) {
        ProposedChangeEntity change = changeRepository.findById(changeId)
                .orElseThrow(() -> new IllegalArgumentException("Change not found: " + changeId));

        if (!"APPROVED".equals(change.getStatus())) {
            throw new IllegalStateException("Change is not approved: " + change.getStatus());
        }

        Map<String, String> credentials = credentialResolver.resolveCredentials(change.getStoreId());

        change.setStatus("APPLYING");
        changeRepository.save(change);
        log.info("Applying change {} (type: {}, entity: {})",
                changeId, change.getChangeType(), change.getShopifyEntityId());

        try {
            // Snapshot current state before applying
            snapshotEntity(change);

            // Apply the change via Shopify
            boolean success = applyToShopify(change, credentials);

            if (success) {
                change.setStatus("APPLIED");
                change.setAppliedAt(LocalDateTime.now());
                change.setRollbackAvailableUntil(LocalDateTime.now().plusDays(30));
                changeRepository.save(change);
                log.info("Change {} applied successfully", changeId);
                return true;
            } else {
                change.setStatus("APPLY_FAILED");
                changeRepository.save(change);
                log.error("Change {} failed to apply to Shopify", changeId);
                return false;
            }

        } catch (Exception e) {
            change.setStatus("APPLY_FAILED");
            changeRepository.save(change);
            log.error("Change {} failed with exception: {}", changeId, e.getMessage(), e);
            return false;
        }
    }

    // â”€â”€â”€ Rollback â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Rollback an applied change using the entity snapshot.
     * Credentials are loaded server-side from the store's stored OAuth token.
     */
    @Transactional
    public boolean rollback(UUID changeId) {
        ProposedChangeEntity change = changeRepository.findById(changeId)
                .orElseThrow(() -> new IllegalArgumentException("Change not found: " + changeId));

        if (!"APPLIED".equals(change.getStatus())) {
            throw new IllegalStateException("Change is not applied: " + change.getStatus());
        }

        Map<String, String> credentials = credentialResolver.resolveCredentials(change.getStoreId());

        if (change.getRollbackAvailableUntil() != null
                && LocalDateTime.now().isAfter(change.getRollbackAvailableUntil())) {
            throw new IllegalStateException("Rollback window has expired for change: " + changeId);
        }

        log.info("Rolling back change {}", changeId);

        EntitySnapshot snapshot = snapshotRepository.findByChangeId(changeId)
                .orElseThrow(() -> new IllegalStateException("No snapshot found for rollback: " + changeId));

        try {
            // Parse snapshot and apply the original value back
            JsonNode snapshotData = objectMapper.readTree(snapshot.getSnapshotData());
            JsonNode originalValue = snapshotData.path("original_value");

            // Build a JsonNode with the original value
            JsonNode rollbackPayload = objectMapper.createObjectNode()
                    .set(getShopifyFieldName(change.getChangeType()), originalValue);

            boolean success = shopifyConnector.patchProduct(
                    change.getShopifyEntityId(), rollbackPayload, credentials);

            if (success) {
                change.setStatus("ROLLED_BACK");
                changeRepository.save(change);
                log.info("Change {} rolled back successfully", changeId);
                return true;
            } else {
                log.error("Failed to rollback change {} on Shopify", changeId);
                return false;
            }

        } catch (Exception e) {
            log.error("Rollback failed for change {}: {}", changeId, e.getMessage(), e);
            return false;
        }
    }

    // â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void snapshotEntity(ProposedChangeEntity change) {
        try {
            JsonNode originalValueNode = objectMapper.readTree(change.getCurrentValue());
            String snapshotData = objectMapper.writeValueAsString(Map.of(
                    "entity_type", change.getShopifyEntityType(),
                    "entity_id", change.getShopifyEntityId(),
                    "change_type", change.getChangeType(),
                    "original_value", originalValueNode
            ));

            EntitySnapshot snapshot = EntitySnapshot.builder()
                    .changeId(change.getChangeId())
                    .entityType(change.getShopifyEntityType())
                    .entityId(change.getShopifyEntityId())
                    .snapshotData(snapshotData)
                    .build();

            snapshotRepository.save(snapshot);
            log.debug("Snapshotted entity {} before change {}", change.getShopifyEntityId(), change.getChangeId());
        } catch (Exception e) {
            log.error("Failed to snapshot entity for change {}: {}", change.getChangeId(), e.getMessage());
            throw new RuntimeException("Snapshot failed â€” aborting change to protect rollback capability", e);
        }
    }

    private boolean applyToShopify(ProposedChangeEntity change, Map<String, String> credentials) {
        String changeType = change.getChangeType();

        if ("PRODUCT_TITLE".equals(changeType) || "PRODUCT_DESCRIPTION".equals(changeType)) {
            try {
                JsonNode value = objectMapper.readTree(change.getProposedValue());
                ObjectNode payload = objectMapper.createObjectNode();
                payload.set(getShopifyFieldName(changeType), value);
                return shopifyConnector.patchProduct(change.getShopifyEntityId(), payload, credentials);
            } catch (Exception e) {
                log.error("Failed to parse proposed value for Shopify write: {}", e.getMessage());
                return false;
            }
        }

        if ("META_TITLE".equals(changeType) || "META_DESCRIPTION".equals(changeType)) {
            try {
                JsonNode value = objectMapper.readTree(change.getProposedValue());
                String metaValue = value.isTextual() ? value.asText() : value.toString();
                // Shopify stores SEO title/description as metafields with namespace "global"
                String key = "META_TITLE".equals(changeType) ? "title_tag" : "description_tag";
                return shopifyConnector.patchMetafield(
                        change.getShopifyEntityId(), "global", key, metaValue, credentials);
            } catch (Exception e) {
                log.error("Failed to apply meta field change: {}", e.getMessage());
                return false;
            }
        }

        if ("PRICE_CHANGE".equals(changeType)) {
            try {
                JsonNode value = objectMapper.readTree(change.getProposedValue());
                String newPrice = value.isTextual() ? value.asText() : value.toString();
                // Price changes go to the product's first variant via product patch
                ObjectNode payload = objectMapper.createObjectNode();
                var variantsArray = objectMapper.createArrayNode();
                ObjectNode variantNode = objectMapper.createObjectNode();
                variantNode.put("price", newPrice);
                variantsArray.add(variantNode);
                payload.set("variants", variantsArray);
                return shopifyConnector.patchProduct(change.getShopifyEntityId(), payload, credentials);
            } catch (Exception e) {
                log.error("Failed to apply price change: {}", e.getMessage());
                return false;
            }
        }

        log.warn("Unknown change type: {} for change {}", changeType, change.getChangeId());
        return false;
    }

    private String getShopifyFieldName(String changeType) {
        return switch (changeType) {
            case "PRODUCT_TITLE" -> "title";
            case "PRODUCT_DESCRIPTION" -> "body_html";
            case "META_TITLE" -> "metafields_global_title_tag";
            case "META_DESCRIPTION" -> "metafields_global_description_tag";
            default -> "unknown";
        };
    }
}

