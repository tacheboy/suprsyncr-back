package com.suprsyncr.product.studio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.product.studio.dto.CreateDraftRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Synchronous client for the Python engine's {@code POST /engine/studio/listing}.
 *
 * The engine call typically takes 4–8s (one premium vision call, three parallel
 * cheap specialists, one verifier). Synchronous keeps the API simple for MVP;
 * if we later want progressive disclosure we can switch to SSE here without
 * touching the controller's contract.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EngineStudioClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${studio.engine-url:http://localhost:8081}")
    private String engineUrl;

    @Value("${studio.budget-inr:3.0}")
    private double budgetInr;

    public JsonNode runStudio(UUID draftId,
                              String storeId,
                              String imageUrl,
                              String claimedTitle,
                              String posture,
                              CreateDraftRequest.Services services) {
        String url = engineUrl + "/engine/studio/listing";
        CreateDraftRequest.Services svc =
                services != null ? services : new CreateDraftRequest.Services();

        Map<String, Object> payload = new HashMap<>();
        payload.put("draft_id", draftId.toString());
        payload.put("store_id", storeId);
        payload.put("image_url", imageUrl);
        payload.put("claimed_title", claimedTitle);
        payload.put("posture", posture == null ? "balanced" : posture);
        payload.put("budget_inr", budgetInr);
        payload.put("services", Map.of(
                "product", svc.isProduct(),
                "seo", svc.isSeo(),
                "platform", svc.isPlatform()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, req, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException(
                        "engine /studio/listing returned " + response.getStatusCode());
            }
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.warn("engine /studio/listing call failed for draft {}: {}",
                    draftId, e.getMessage());
            throw new IllegalStateException("engine call failed: " + e.getMessage(), e);
        }
    }
}
