package com.suprsyncr.autopilot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.autopilot.domain.AgentRun;
import com.suprsyncr.autopilot.domain.ChangeImpact;
import com.suprsyncr.autopilot.domain.ProposedChangeEntity;
import com.suprsyncr.autopilot.dto.AgentRunDto;
import com.suprsyncr.autopilot.dto.ImpactDto;
import com.suprsyncr.autopilot.dto.ProposedChangeDto;
import com.suprsyncr.autopilot.repository.ProposedChangeRepository;
import com.suprsyncr.autopilot.service.AgentRunOrchestratorService;
import com.suprsyncr.autopilot.service.ChangeManagementService;
import com.suprsyncr.autopilot.service.ImpactTrackerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Autopilot API Controller â€” the full REST API for the agent pipeline.
 *
 * Endpoints:
 *   Agent Runs:
 *     POST /api/autopilot/run/{storeId}              â†’ trigger agent run
 *     GET  /api/autopilot/runs/{storeId}              â†’ list past runs
 *     GET  /api/autopilot/run/{runId}/status           â†’ run status
 *
 *   Proposal Ingestion (internal, from Python service):
 *     POST /api/autopilot/proposals/batch              â†’ batch ingest proposals
 *
 *   Approval Queue:
 *     GET  /api/autopilot/proposals/{storeId}          â†’ pending proposals
 *     GET  /api/autopilot/proposals/{storeId}/history  â†’ all proposals
 *
 *   Change Lifecycle:
 *     POST /api/autopilot/changes/{changeId}/approve   â†’ approve
 *     POST /api/autopilot/changes/{changeId}/reject    â†’ reject
 *     POST /api/autopilot/changes/{changeId}/apply     â†’ execute on Shopify
 *     POST /api/autopilot/changes/{changeId}/undo      â†’ rollback
 *     POST /api/autopilot/changes/batch-approve        â†’ batch approve LOW risk
 *
 *   Impact Lab:
 *     GET  /api/autopilot/impact/{storeId}             â†’ impact data
 */
@RestController
@RequestMapping("/api/autopilot")
@RequiredArgsConstructor
@Slf4j
public class AutopilotController {

    private final AgentRunOrchestratorService runOrchestratorService;
    private final ChangeManagementService changeManagementService;
    private final ImpactTrackerService impactTrackerService;
    private final ProposedChangeRepository proposedChangeRepository;
    private final ObjectMapper objectMapper;

    // â”€â”€â”€ Agent Runs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PostMapping("/run/{storeId}")
    public ResponseEntity<AgentRunDto> triggerRun(@PathVariable String storeId) {
        AgentRun run = runOrchestratorService.startRun(storeId, "MANUAL");
        runOrchestratorService.executeRunAsync(run);
        return ResponseEntity.accepted().body(toRunDto(run));
    }

    /**
     * Trigger an individual service run with specific agents.
     * Supports selective agent execution without full pipeline overhead.
     */
    @PostMapping("/service/{storeId}")
    public ResponseEntity<AgentRunDto> triggerServiceRun(
            @PathVariable String storeId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> agents = (List<String>) body.getOrDefault("agents", List.of());
        @SuppressWarnings("unchecked")
        List<String> productIds = (List<String>) body.get("productIds");

        if (agents.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Validate agent names
        List<String> validAgents = List.of("seo", "listing", "pricing", "cart_recovery", "competitor_intel");
        for (String agent : agents) {
            if (!validAgents.contains(agent.toLowerCase())) {
                return ResponseEntity.badRequest().build();
            }
        }

        AgentRun run = runOrchestratorService.startIndividualRun(storeId, agents, productIds);
        runOrchestratorService.executeRunAsync(run);
        return ResponseEntity.accepted().body(toRunDto(run));
    }

    /**
     * Get per-service analytics preview for the services page cards.
     */
    @GetMapping("/services/{storeId}/preview")
    public ResponseEntity<Map<String, Object>> getServicesPreview(@PathVariable String storeId) {
        Map<String, Object> preview = runOrchestratorService.getServicesPreview(storeId);
        return ResponseEntity.ok(preview);
    }

    @GetMapping("/runs/{storeId}")
    public ResponseEntity<List<AgentRunDto>> listRuns(@PathVariable String storeId) {
        List<AgentRun> runs = runOrchestratorService.getRunsForStore(storeId);
        List<AgentRunDto> dtos = runs.stream().map(this::toRunDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/run/{runId}/status")
    public ResponseEntity<AgentRunDto> getRunStatus(@PathVariable UUID runId) {
        AgentRun run = runOrchestratorService.getRun(runId);
        if (run == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toRunDto(run));
    }

    // â”€â”€â”€ Proposal Ingestion (internal) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Batch ingest proposals from the Python agent service.
     * This is the callback endpoint that Python calls when a run completes.
     */
    @PostMapping("/proposals/batch")
    public ResponseEntity<Map<String, Object>> ingestBatchProposals(@RequestBody JsonNode payload) {
        String runIdStr = payload.path("runId").asText();
        String storeId = payload.path("storeId").asText();
        JsonNode proposalsNode = payload.path("proposals");

        if (!proposalsNode.isArray()) {
            return ResponseEntity.badRequest().body(Map.of("error", "proposals must be an array"));
        }

        List<ProposedChangeEntity> entities = new ArrayList<>();
        BigDecimal totalImpact = BigDecimal.ZERO;

        for (JsonNode p : proposalsNode) {
            try {
                ProposedChangeEntity entity = ProposedChangeEntity.builder()
                        .changeId(UUID.fromString(p.path("changeId").asText(UUID.randomUUID().toString())))
                        .storeId(storeId)
                        .runId(UUID.fromString(runIdStr))
                        .agentType(p.path("agent_type").asText())
                        .changeType(p.path("change_type").asText())
                        .shopifyEntityType(p.path("shopify_entity_type").asText("product"))
                        .shopifyEntityId(p.path("shopify_entity_id").asText())
                        .currentValue(objectMapper.writeValueAsString(p.path("current_value")))
                        .proposedValue(objectMapper.writeValueAsString(p.path("proposed_value")))
                        .agentReasoning(p.path("agent_reasoning").asText())
                        .estimatedImpact(p.has("estimated_impact")
                                ? objectMapper.writeValueAsString(p.path("estimated_impact")) : null)
                        .riskLevel(p.path("risk_level").asText("LOW"))
                        .riskNotes(p.path("risk_notes").asText(""))
                        .isTest(p.path("is_test").asBoolean(false))
                        .build();

                entities.add(entity);

                // Accumulate impact
                if (p.has("estimated_impact")) {
                    double lift = p.path("estimated_impact").path("revenue_lift_inr").asDouble(0);
                    totalImpact = totalImpact.add(BigDecimal.valueOf(lift));
                }
            } catch (Exception e) {
                log.warn("Skipping invalid proposal: {}", e.getMessage());
            }
        }

        List<ProposedChangeEntity> saved = changeManagementService.proposeBatch(entities);

        // Mark the run as complete
        try {
            UUID runId = UUID.fromString(runIdStr);
            runOrchestratorService.markRunComplete(runId, saved.size(), totalImpact);
        } catch (Exception e) {
            log.warn("Failed to mark run complete: {}", e.getMessage());
        }

        log.info("Ingested {} proposals for store {} (run {})", saved.size(), storeId, runIdStr);

        return ResponseEntity.ok(Map.of(
                "ingested", saved.size(),
                "runId", runIdStr,
                "estimatedImpactInr", totalImpact
        ));
    }

    // â”€â”€â”€ Approval Queue â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/proposals/{storeId}")
    public ResponseEntity<List<ProposedChangeDto>> getPendingProposals(@PathVariable String storeId) {
        List<ProposedChangeEntity> proposals = changeManagementService.getPendingProposals(storeId);
        return ResponseEntity.ok(proposals.stream().map(this::toChangeDto).collect(Collectors.toList()));
    }

    @GetMapping("/proposals/{storeId}/history")
    public ResponseEntity<List<ProposedChangeDto>> getProposalHistory(@PathVariable String storeId) {
        List<ProposedChangeEntity> proposals = changeManagementService.getAllProposals(storeId);
        return ResponseEntity.ok(proposals.stream().map(this::toChangeDto).collect(Collectors.toList()));
    }

    // â”€â”€â”€ Change Lifecycle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PostMapping("/changes/{changeId}/approve")
    public ResponseEntity<ProposedChangeDto> approveChange(
            @PathVariable UUID changeId,
            @RequestBody(required = false) Map<String, String> body) {
        String approvedBy = body != null ? body.getOrDefault("approvedBy", "seller") : "seller";
        ProposedChangeEntity change = changeManagementService.approve(changeId, approvedBy);
        return ResponseEntity.ok(toChangeDto(change));
    }

    @PostMapping("/changes/{changeId}/reject")
    public ResponseEntity<ProposedChangeDto> rejectChange(@PathVariable UUID changeId) {
        ProposedChangeEntity change = changeManagementService.reject(changeId);
        return ResponseEntity.ok(toChangeDto(change));
    }

    @PostMapping("/changes/{changeId}/apply")
    public ResponseEntity<Map<String, Object>> applyChange(@PathVariable UUID changeId) {
        // Credentials are loaded server-side from the store's stored OAuth token.
        boolean success = changeManagementService.apply(changeId);
        return ResponseEntity.ok(Map.of("success", success, "changeId", changeId.toString()));
    }

    @PostMapping("/changes/{changeId}/undo")
    public ResponseEntity<Map<String, Object>> undoChange(@PathVariable UUID changeId) {
        // Credentials are loaded server-side from the store's stored OAuth token.
        boolean success = changeManagementService.rollback(changeId);
        return ResponseEntity.ok(Map.of("success", success, "changeId", changeId.toString()));
    }

    @PostMapping("/changes/batch-approve")
    public ResponseEntity<Map<String, Object>> batchApproveLowRisk(@RequestBody Map<String, String> body) {
        String storeId = body.get("storeId");
        String approvedBy = body.getOrDefault("approvedBy", "seller");
        int count = changeManagementService.batchApproveLowRisk(storeId, approvedBy);
        return ResponseEntity.ok(Map.of("approved", count, "storeId", storeId));
    }

    // â”€â”€â”€ Impact Lab â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/impact/{storeId}")
    public ResponseEntity<List<ImpactDto>> getImpact(@PathVariable String storeId) {
        List<ChangeImpact> impacts = impactTrackerService.getImpactForStore(storeId);
        List<ImpactDto> dtos = impacts.stream().map(this::toImpactDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // â”€â”€â”€ Mapping Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private AgentRunDto toRunDto(AgentRun run) {
        return AgentRunDto.builder()
                .runId(run.getRunId())
                .storeId(run.getStoreId())
                .triggeredBy(run.getTriggeredBy())
                .triggeredAt(run.getTriggeredAt())
                .status(run.getStatus())
                .runType(run.getRunType())
                .selectedAgents(run.getSelectedAgents())
                .proposalsGenerated(run.getProposalsGenerated())
                .estimatedImpactInr(run.getEstimatedImpactInr())
                .completedAt(run.getCompletedAt())
                .errorMessage(run.getErrorMessage())
                .build();
    }

    private ProposedChangeDto toChangeDto(ProposedChangeEntity entity) {
        ProposedChangeDto dto = ProposedChangeDto.builder()
                .changeId(entity.getChangeId())
                .storeId(entity.getStoreId())
                .runId(entity.getRunId())
                .agentType(entity.getAgentType())
                .changeType(entity.getChangeType())
                .shopifyEntityType(entity.getShopifyEntityType())
                .shopifyEntityId(entity.getShopifyEntityId())
                .agentReasoning(entity.getAgentReasoning())
                .riskLevel(entity.getRiskLevel())
                .riskNotes(entity.getRiskNotes())
                .status(entity.getStatus())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .appliedAt(entity.getAppliedAt())
                .rollbackAvailableUntil(entity.getRollbackAvailableUntil())
                .isTest(entity.getIsTest())
                .testRevertAt(entity.getTestRevertAt())
                .build();

        // Parse JSON fields for DTO
        try {
            if (entity.getCurrentValue() != null)
                dto.setCurrentValue(objectMapper.readTree(entity.getCurrentValue()));
            if (entity.getProposedValue() != null)
                dto.setProposedValue(objectMapper.readTree(entity.getProposedValue()));
            if (entity.getEstimatedImpact() != null)
                dto.setEstimatedImpact(objectMapper.readTree(entity.getEstimatedImpact()));
        } catch (Exception e) {
            log.warn("Failed to parse JSON fields for change {}: {}", entity.getChangeId(), e.getMessage());
        }

        return dto;
    }

    private ImpactDto toImpactDto(ChangeImpact impact) {
        ImpactDto.ImpactDtoBuilder builder = ImpactDto.builder()
                .impactId(impact.getImpactId())
                .changeId(impact.getChangeId())
                .storeId(impact.getStoreId())
                .metricType(impact.getMetricType())
                .baselinePeriodStart(impact.getBaselinePeriodStart())
                .baselinePeriodEnd(impact.getBaselinePeriodEnd())
                .baselineValue(impact.getBaselineValue())
                .measurementPeriodStart(impact.getMeasurementPeriodStart())
                .measurementPeriodEnd(impact.getMeasurementPeriodEnd())
                .measuredValue(impact.getMeasuredValue())
                .deltaAbsolute(impact.getDeltaAbsolute())
                .deltaPercent(impact.getDeltaPercent())
                .attributionConfidence(impact.getAttributionConfidence())
                .attributionNotes(impact.getAttributionNotes())
                .estimatedRevenueImpactInr(impact.getEstimatedRevenueImpactInr());

        // Enrich with change metadata for Impact Lab display
        try {
            proposedChangeRepository.findById(impact.getChangeId()).ifPresent(change -> {
                builder.changeType(change.getChangeType());
                builder.agentType(change.getAgentType());
            });
        } catch (Exception e) {
            log.debug("Could not enrich impact with change metadata: {}", e.getMessage());
        }

        return builder.build();
    }
}

