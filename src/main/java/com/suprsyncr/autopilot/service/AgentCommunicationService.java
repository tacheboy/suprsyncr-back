package com.suprsyncr.autopilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Handles HTTP communication between Spring Boot and the Python Agent Service.
 *
 * Responsibilities:
 * - Build full analytics context from AnalyticsOrchestrator
 * - POST context to Python /agent/run/{storeId}
 * - Support selective agent runs via ?agents= query param
 * - Poll/check agent run status
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentCommunicationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${autopilot.agent-service-url:http://localhost:8081}")
    private String pythonServiceUrl;

    /**
     * Trigger an agent run on the Python service with optional agent selection and product overrides.
     *
     * @param runId             The Spring-side AgentRun id. The Python service uses this same id
     *                          and echoes it back in its /proposals/batch callback, so the run and
     *                          its proposals reconcile against the agent_runs row (no orphan runs).
     * @param storeId           The store to run agents for
     * @param analyticsContext  Pre-built analytics payload
     * @param agents            Optional list of agent names for selective mode (null = full pipeline)
     * @param productOverrides  Optional list of product IDs to focus on
     * @return Run ID accepted by the Python service (echoes runId), or null on failure
     */
    public String triggerAgentRun(String runId, String storeId, Map<String, Object> analyticsContext,
                                   List<String> agents, List<String> productOverrides) {
        String url = pythonServiceUrl + "/agent/run/" + storeId;

        // Add agents query parameter for selective mode
        if (agents != null && !agents.isEmpty()) {
            url += "?agents=" + String.join(",", agents);
        }

        log.info("Triggering Python Agent Service for runId: {} storeId: {} at {} (mode: {})",
                runId, storeId, url, agents != null ? "INDIVIDUAL" : "FULL_PIPELINE");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("run_id", runId);
            payload.put("analytics_context", analyticsContext);
            if (productOverrides != null && !productOverrides.isEmpty()) {
                payload.put("product_overrides", productOverrides);
            }
            String body = objectMapper.writeValueAsString(payload);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String acceptedRunId = responseJson.path("run_id").asText();
                log.info("Python Agent Service accepted run: {} for store: {}", acceptedRunId, storeId);
                return acceptedRunId;
            } else {
                log.error("Python Agent Service returned non-2xx: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to communicate with Python Agent Service at {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Check if the Python agent service is healthy.
     */
    public boolean isAgentServiceHealthy() {
        try {
            String url = pythonServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Python Agent Service health check failed: {}", e.getMessage());
            return false;
        }
    }
}

