package com.suprsyncr.autopilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.autopilot.domain.AgentRun;
import com.suprsyncr.autopilot.domain.AgentTaskEntity;
import com.suprsyncr.autopilot.domain.ModelInvocationEntity;
import com.suprsyncr.autopilot.domain.ProposedChangeEntity;
import com.suprsyncr.autopilot.repository.AgentRunRepository;
import com.suprsyncr.autopilot.repository.AgentTaskRepository;
import com.suprsyncr.autopilot.repository.ModelInvocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the Python Inference Engine ({@code POST :8081/engine/run}) and persists
 * the full result: grounded proposals (with provenance), the planned task graph,
 * the per-model-call cost ledger, and the run-level rupee total.
 *
 * Synchronous on purpose for Phase 0 — the engine returns proposals + telemetry
 * in one response, which keeps the end-to-end path easy to reason about and
 * verify. (The async 202 + callback variant from the strategy doc can be layered
 * on later without changing persistence.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EngineRunService {

    private static final String ENGINE_VERSION = "0.1.0";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ChangeManagementService changeManagementService;
    private final AgentRunRepository agentRunRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final ModelInvocationRepository modelInvocationRepository;

    @Value("${autopilot.agent-service-url:http://localhost:8081}")
    private String engineUrl;

    @Value("${autopilot.engine.budget-inr:5.0}")
    private double budgetInr;

    /**
     * Run the engine for a store and persist everything it returns.
     *
     * @return true on success (run marked COMPLETE), false if the engine was
     *         unreachable/errored so the caller can fall back to the stub.
     */
    public boolean runAndPersist(AgentRun run, Map<String, Object> evidencePack) {
        // When the orchestrator builds evidence from real synced catalogue it tags
        // the product_health section with dataSource="live_shopify_catalogue".
        // Proposals from real catalogue should be applyable on real Shopify;
        // proposals from the demo bootstrap must not push to real Shopify (their
        // entity ids don't exist there) — so isTest is set per-run accordingly.
        boolean liveEvidence = isLiveCatalogueEvidence(evidencePack);
        return runAndPersist(run, evidencePack, liveEvidence);
    }

    @SuppressWarnings("unchecked")
    private boolean isLiveCatalogueEvidence(Map<String, Object> evidencePack) {
        Object ph = evidencePack.get("product_health");
        if (ph instanceof Map) {
            return "live_shopify_catalogue".equals(((Map<String, Object>) ph).get("dataSource"));
        }
        return false;
    }

    private boolean runAndPersist(AgentRun run, Map<String, Object> evidencePack, boolean liveEvidence) {
        String url = engineUrl + "/engine/run";
        JsonNode resp;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("run_id", run.getRunId().toString());
            payload.put("store_id", run.getStoreId());
            payload.put("evidence_pack", evidencePack);
            payload.put("budget_inr", budgetInr);
            payload.put("posture", "balanced");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, req, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Engine returned non-2xx ({}) for run {}", response.getStatusCode(), run.getRunId());
                return false;
            }
            resp = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.warn("Engine call failed for run {} at {}: {}", run.getRunId(), url, e.getMessage());
            return false;
        }

        try {
            persistTelemetry(run, resp.path("telemetry"));
            int proposalCount = persistProposals(run, resp.path("proposals"), liveEvidence);

            JsonNode telemetry = resp.path("telemetry");
            BigDecimal totalCost = telemetry.path("total_cost_inr").isMissingNode()
                    ? BigDecimal.ZERO : BigDecimal.valueOf(telemetry.path("total_cost_inr").asDouble(0));
            BigDecimal impact = BigDecimal.valueOf(resp.path("estimated_impact_inr").asDouble(0));

            run.setStatus(mapStatus(resp.path("status").asText("COMPLETE")));
            run.setProposalsGenerated(proposalCount);
            run.setEstimatedImpactInr(impact);
            run.setTotalCostInr(totalCost);
            run.setEngineVersion(ENGINE_VERSION);
            run.setOrchestratorReasoning(toJson(telemetry.path("plan_reasoning").asText("")));
            run.setCompletedAt(LocalDateTime.now());
            agentRunRepository.save(run);

            log.info("Engine run {} persisted: {} proposals, cost ₹{}, impact ₹{}, status {}",
                    run.getRunId(), proposalCount, totalCost, impact, run.getStatus());
            return true;
        } catch (Exception e) {
            log.error("Failed to persist engine result for run {}: {}", run.getRunId(), e.getMessage(), e);
            return false;
        }
    }

    private int persistProposals(AgentRun run, JsonNode proposals, boolean liveEvidence) {
        if (!proposals.isArray()) return 0;
        List<ProposedChangeEntity> entities = new ArrayList<>();
        for (JsonNode p : proposals) {
            try {
                JsonNode impact = p.path("estimated_impact");
                ProposedChangeEntity e = ProposedChangeEntity.builder()
                        .changeId(UUID.randomUUID())
                        .storeId(run.getStoreId())
                        .runId(run.getRunId())
                        .agentType(p.path("agent_type").asText())
                        .changeType(p.path("change_type").asText())
                        .shopifyEntityType(p.path("shopify_entity_type").asText("product"))
                        .shopifyEntityId(p.path("shopify_entity_id").asText())
                        .currentValue(objectMapper.writeValueAsString(p.path("current_value")))
                        .proposedValue(objectMapper.writeValueAsString(p.path("proposed_value")))
                        .agentReasoning(p.path("agent_reasoning").asText(""))
                        .estimatedImpact(impact.isMissingNode() ? null : objectMapper.writeValueAsString(impact))
                        .riskLevel(p.path("risk_level").asText("LOW"))
                        .riskNotes(p.path("risk_notes").asText(""))
                        .evidenceIds(objectMapper.writeValueAsString(p.path("evidence_ids")))
                        .modelPath(p.path("model_path").asText(""))
                        .costInr(BigDecimal.valueOf(p.path("cost_inr").asDouble(0)))
                        .confidence(BigDecimal.valueOf(p.path("confidence").asDouble(0)))
                        // Proposals built from real synced catalogue target real
                        // Shopify entity ids → apply pushes to Shopify. Demo-
                        // bootstrap proposals must simulate (their entity ids
                        // don't exist on the seller's real storefront).
                        .isTest(!liveEvidence)
                        .build();
                entities.add(e);
            } catch (Exception ex) {
                log.warn("Skipping invalid engine proposal: {}", ex.getMessage());
            }
        }
        return changeManagementService.proposeBatch(entities).size();
    }

    private void persistTelemetry(AgentRun run, JsonNode telemetry) {
        // tasks
        JsonNode tasks = telemetry.path("tasks");
        if (tasks.isArray()) {
            List<AgentTaskEntity> taskEntities = new ArrayList<>();
            for (JsonNode t : tasks) {
                taskEntities.add(AgentTaskEntity.builder()
                        .taskId(parseUuid(t.path("task_id").asText()))
                        .runId(run.getRunId())
                        .storeId(run.getStoreId())
                        .agentType(t.path("agent").asText())
                        .kind(t.path("kind").asText())
                        .complexity(t.path("complexity").asText(null))
                        .status(t.path("status").asText(null))
                        .revenueAtStakeInr(BigDecimal.valueOf(t.path("revenue_at_stake_inr").asDouble(0)))
                        .accuracyBar(BigDecimal.valueOf(t.path("accuracy_bar").asDouble(0)))
                        .modelPath(t.path("model_path").asText(""))
                        .costInr(BigDecimal.valueOf(t.path("cost_inr").asDouble(0)))
                        .confidence(t.path("confidence").isNull() ? null
                                : BigDecimal.valueOf(t.path("confidence").asDouble(0)))
                        .note(t.path("note").asText(""))
                        .build());
            }
            agentTaskRepository.saveAll(taskEntities);
        }

        // model invocations
        JsonNode invs = telemetry.path("invocations");
        if (invs.isArray()) {
            List<ModelInvocationEntity> invEntities = new ArrayList<>();
            for (JsonNode i : invs) {
                invEntities.add(ModelInvocationEntity.builder()
                        .id(UUID.randomUUID())
                        .runId(run.getRunId())
                        .taskId(parseUuid(i.path("task_id").asText()))
                        .model(i.path("model").asText())
                        .tier((short) i.path("tier").asInt(0))
                        .purpose(i.path("purpose").asText(null))
                        .promptTokens(i.path("prompt_tokens").asInt(0))
                        .outputTokens(i.path("completion_tokens").asInt(0))
                        .latencyMs(i.path("latency_ms").asInt(0))
                        .costInr(BigDecimal.valueOf(i.path("cost_inr").asDouble(0)))
                        .verifierPassed(i.path("verifier_passed").isNull() ? null
                                : i.path("verifier_passed").asBoolean(false))
                        .escalatedFrom(i.path("escalated_from").asText(null))
                        .confidence(i.path("confidence").isNull() ? null
                                : BigDecimal.valueOf(i.path("confidence").asDouble(0)))
                        .ok(i.path("ok").asBoolean(true))
                        .error(i.path("error").asText(null))
                        .build());
            }
            modelInvocationRepository.saveAll(invEntities);
        }
    }

    private String mapStatus(String engineStatus) {
        // engine COMPLETE/PARTIAL/FAILED → run COMPLETE/FAILED
        return "FAILED".equals(engineStatus) ? "FAILED" : "COMPLETE";
    }

    private UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(String text) {
        try {
            return objectMapper.writeValueAsString(text);
        } catch (Exception e) {
            return "\"\"";
        }
    }
}
