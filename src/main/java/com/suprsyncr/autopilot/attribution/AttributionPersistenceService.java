package com.suprsyncr.autopilot.attribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.suprsyncr.autopilot.domain.ChangeImpact;
import com.suprsyncr.autopilot.domain.ProposedChangeEntity;
import com.suprsyncr.autopilot.repository.ChangeImpactRepository;
import com.suprsyncr.autopilot.repository.ProposedChangeRepository;
import com.suprsyncr.notification.entity.NotificationType;
import com.suprsyncr.notification.service.StoreNotificationService;
import com.suprsyncr.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns every DB write in the attribution pipeline. Kept in a separate bean so
 * Spring's @Transactional proxy is active on each public method — calling
 * these via this. inside AttributionService would bypass the proxy and make
 * the annotations no-ops.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttributionPersistenceService {

    private final AttributionRepository attributionRepository;
    private final ProposedChangeRepository proposedChangeRepository;
    private final ChangeImpactRepository changeImpactRepository;
    private final ObjectMapper objectMapper;
    private final StoreNotificationService notificationService;

    public Attribution persistGateSkipped(Order order) {
        Attribution row = baseRow(order, null)
                .status(AttributionStatus.GATE_SKIPPED)
                .build();
        row.setCompletedAt(LocalDateTime.now());
        return attributionRepository.save(row);
    }

    public Attribution persistPending(Order order, Long productId) {
        Attribution row = baseRow(order, productId)
                .status(AttributionStatus.PENDING)
                .build();
        return attributionRepository.save(row);
    }

    public Attribution markFailed(UUID attributionId, String message) {
        Attribution row = attributionRepository.findById(attributionId)
                .orElseThrow(() -> new IllegalStateException(
                        "attribution vanished: " + attributionId));
        row.setStatus(AttributionStatus.FAILED);
        row.setErrorMessage(message);
        row.setCompletedAt(LocalDateTime.now());
        return attributionRepository.save(row);
    }

    public Attribution persistEngineResult(UUID attributionId, Order order, JsonNode resp) {
        Attribution row = attributionRepository.findById(attributionId)
                .orElseThrow(() -> new IllegalStateException(
                        "attribution vanished: " + attributionId));

        String engineStatus = resp.path("status").asText("FAILED");
        JsonNode verdict = resp.path("verdict");
        boolean attributed = verdict.path("attributed").asBoolean(false);

        row.setStatus(mapStatus(engineStatus, attributed));
        row.setCausalChangeId(parseUuid(verdict.path("causal_change_id")));
        row.setCausalChangeType(textOrNull(verdict.path("causal_change_type")));
        row.setConfidence(decimalOrNull(verdict.path("confidence")));
        row.setReasoning(textOrNull(verdict.path("reasoning")));
        row.setPattern(writeNode(verdict.path("pattern")));

        row.setRealizedRevenueInr(decimalOrZero(resp.path("realized_revenue_inr")));
        row.setForecastedLiftInr(decimalOrZero(resp.path("forecasted_lift_inr")));

        row.setTotalCostInr(decimalOrNull(resp.path("total_cost_inr")));
        row.setModelPath(textOrNull(resp.path("model_path")));
        row.setPlanReasoning(textOrNull(resp.path("plan_reasoning")));
        row.setInvocations(writeNode(resp.path("invocations")));
        row.setVerifier(writeNode(resp.path("verifier")));

        List<UUID> generatedIds = persistProposals(row, order, resp.path("proposals"));
        row.setProposalsGenerated(generatedIds.size());
        if (!generatedIds.isEmpty()) {
            ArrayNode arr = objectMapper.createArrayNode();
            generatedIds.forEach(id -> arr.add(id.toString()));
            row.setGeneratedChangeIds(arr.toString());
        }

        if (row.getStatus() == AttributionStatus.ATTRIBUTED && row.getCausalChangeId() != null) {
            creditRealizedLift(row.getCausalChangeId(), row.getStoreId(),
                    row.getRealizedRevenueInr());
        }

        row.setCompletedAt(LocalDateTime.now());
        Attribution saved = attributionRepository.save(row);

        if (saved.getStatus() == AttributionStatus.ATTRIBUTED && order.getPlatform() != null) {
            try {
                String summary = saved.getCausalChangeType() != null
                        ? "Sale attributed to: " + saved.getCausalChangeType()
                        : "Sale attributed via autopilot";
                notificationService.createInternalNotification(
                        order.getPlatform(), NotificationType.ATTRIBUTION_COMPLETED,
                        "internal/attribution",
                        String.valueOf(order.getId()),
                        summary,
                        saved.getRealizedRevenueInr(),
                        "INR");
            } catch (Exception e) {
                log.warn("attribution notification skipped: {}", e.getMessage());
            }
        }

        return saved;
    }

    // ---- private helpers ------------------------------------------------------

    private Attribution.AttributionBuilder baseRow(Order order, Long productId) {
        String storeId = order.getPlatform() != null
                ? String.valueOf(order.getPlatform().getId())
                : "unknown";
        return Attribution.builder()
                .orderId(order.getId())
                .storeId(storeId)
                .productId(productId)
                .orderRevenueInr(order.getTotalAmount() != null
                        ? order.getTotalAmount() : BigDecimal.ZERO);
    }

    private List<UUID> persistProposals(Attribution row, Order order, JsonNode proposals) {
        if (proposals == null || !proposals.isArray() || proposals.isEmpty()) {
            return List.of();
        }
        List<ProposedChangeEntity> out = new ArrayList<>();
        for (JsonNode p : proposals) {
            String productId = p.path("product_id").asText("");
            if (productId.isBlank()) continue;
            try {
                ProposedChangeEntity e = ProposedChangeEntity.builder()
                        .storeId(row.getStoreId())
                        .agentType("ATTRIBUTION_PROPAGATION")
                        .changeType(p.path("change_type").asText(""))
                        .shopifyEntityType("product")
                        .shopifyEntityId(productId)
                        .currentValue(writeNodeOr(p.path("current_value"), "null"))
                        .proposedValue(writeNodeOr(p.path("proposed_value"), "null"))
                        .agentReasoning(p.path("reasoning").asText(""))
                        .estimatedImpact(objectMapper.writeValueAsString(Map.of(
                                "revenue_lift_inr", p.path("estimated_lift_inr").asDouble(0),
                                "confidence", p.path("confidence").asDouble(0),
                                "source", "attribution",
                                "from_order_id", order.getId(),
                                "from_attribution_id", row.getAttributionId().toString()
                        )))
                        .riskLevel(p.path("risk_level").asText("LOW"))
                        .riskNotes("Propagated from sale on order " + order.getId())
                        .modelPath(row.getModelPath())
                        .costInr(BigDecimal.ZERO)
                        .confidence(decimalOrZero(p.path("confidence")))
                        .build();
                out.add(e);
            } catch (Exception ex) {
                log.warn("skipping unparseable attribution proposal: {}", ex.getMessage());
            }
        }
        List<ProposedChangeEntity> saved = proposedChangeRepository.saveAll(out);
        return saved.stream().map(ProposedChangeEntity::getChangeId).toList();
    }

    private void creditRealizedLift(UUID causalChangeId, String storeId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return;
        Optional<ChangeImpact> existing = changeImpactRepository.findAll().stream()
                .filter(ci -> causalChangeId.equals(ci.getChangeId()))
                .findFirst();
        if (existing.isPresent()) {
            ChangeImpact ci = existing.get();
            BigDecimal base = ci.getEstimatedRevenueImpactInr() != null
                    ? ci.getEstimatedRevenueImpactInr() : BigDecimal.ZERO;
            ci.setEstimatedRevenueImpactInr(base.add(amount));
            ci.setComputedAt(LocalDateTime.now());
            ci.setAttributionNotes(appendNote(ci.getAttributionNotes(),
                    "+" + amount + " from attributed order"));
            changeImpactRepository.save(ci);
        } else {
            ChangeImpact ci = ChangeImpact.builder()
                    .changeId(causalChangeId)
                    .storeId(storeId)
                    .metricType("attributed_revenue")
                    .estimatedRevenueImpactInr(amount)
                    .attributionConfidence("HIGH")
                    .attributionNotes("Realised revenue credited via attribution pipeline")
                    .build();
            changeImpactRepository.save(ci);
        }
    }

    private static AttributionStatus mapStatus(String engineStatus, boolean attributed) {
        return switch (engineStatus) {
            case "COMPLETE", "PARTIAL" -> attributed
                    ? AttributionStatus.ATTRIBUTED : AttributionStatus.NOT_ATTRIBUTABLE;
            case "NOT_ATTRIBUTABLE" -> AttributionStatus.NOT_ATTRIBUTABLE;
            default -> AttributionStatus.FAILED;
        };
    }

    private String writeNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        try { return objectMapper.writeValueAsString(node); } catch (Exception e) { return null; }
    }

    private String writeNodeOr(JsonNode node, String fallback) {
        String w = writeNode(node);
        return w != null ? w : fallback;
    }

    private static String textOrNull(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return null;
        return n.isTextual() ? n.asText() : n.toString();
    }

    private static UUID parseUuid(JsonNode n) {
        if (n == null || !n.isTextual()) return null;
        String s = n.asText();
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }

    private static BigDecimal decimalOrNull(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull() || !n.isNumber()) return null;
        return BigDecimal.valueOf(n.asDouble());
    }

    private static BigDecimal decimalOrZero(JsonNode n) {
        BigDecimal d = decimalOrNull(n);
        return d != null ? d : BigDecimal.ZERO;
    }

    private static String appendNote(String existing, String addition) {
        if (existing == null || existing.isBlank()) return addition;
        return existing + "; " + addition;
    }
}
