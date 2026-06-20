package com.suprsyncr.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.ai.client.OpenAiClient;
import com.suprsyncr.ai.client.OpenAiResponse;
import com.suprsyncr.ai.domain.AiRequest;
import com.suprsyncr.ai.dto.ContentOptimizeRequest;
import com.suprsyncr.ai.dto.ContentOptimizeResponse;
import com.suprsyncr.ai.dto.PlatformSuggestRequest;
import com.suprsyncr.ai.dto.PlatformSuggestResponse;
import com.suprsyncr.ai.dto.ProductGenerateRequest;
import com.suprsyncr.ai.dto.ProductGenerateResponse;
import com.suprsyncr.ai.exception.AiException;
import com.suprsyncr.ai.exception.AiParseException;
import com.suprsyncr.ai.prompt.AmazonOptimizePrompt;
import com.suprsyncr.ai.prompt.PlatformSuggesterPrompt;
import com.suprsyncr.ai.prompt.ProductGeneratorPrompt;
import com.suprsyncr.ai.repository.AiRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentOptimizerService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final AiRequestRepository aiRequestRepository;

    public ContentOptimizeResponse optimizeForAmazon(Long sellerId, ContentOptimizeRequest request, MultipartFile image) {
        OpenAiResponse response = null;
        try {
            String system = AmazonOptimizePrompt.system();
            String user = AmazonOptimizePrompt.user(request);

            byte[] imageBytes = image != null ? image.getBytes() : new byte[0];
            String mimeType = image != null && image.getContentType() != null ? image.getContentType() : "image/jpeg";

            response = imageBytes.length > 0
                    ? openAiClient.callVision(system, user, imageBytes, mimeType)
                    : openAiClient.callText(system, user);

            log.info("OpenAI CONTENT_OPTIMIZE responded in {}ms", response.getLatencyMs());
            ContentOptimizeResponse result = objectMapper.readValue(response.getText(), ContentOptimizeResponse.class);
            result.setLatencyMs(response.getLatencyMs());
            saveAiRequest(sellerId, "CONTENT_OPTIMIZE", "Optimized: " + request.getCurrentTitle(), response, "SUCCESS");
            return result;

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            saveAiRequestSafe(sellerId, "CONTENT_OPTIMIZE", "Failed: " + request.getCurrentTitle());
            log.error("Failed to parse ContentOptimizeResponse. Raw: {}", response != null ? response.getText() : "<no response>", e);
            throw new AiParseException("Could not parse AI response", e);
        } catch (AiException e) {
            saveAiRequestSafe(sellerId, "CONTENT_OPTIMIZE", "Failed: " + request.getCurrentTitle());
            throw e;
        } catch (Exception e) {
            saveAiRequestSafe(sellerId, "CONTENT_OPTIMIZE", "Failed: " + request.getCurrentTitle());
            throw new RuntimeException(e);
        }
    }

    public ProductGenerateResponse generateProduct(Long sellerId, ProductGenerateRequest request, MultipartFile image) {
        OpenAiResponse response = null;
        try {
            String system = ProductGeneratorPrompt.system();
            String user = ProductGeneratorPrompt.user(request);

            byte[] imageBytes = image != null ? image.getBytes() : new byte[0];
            String mimeType = image != null && image.getContentType() != null ? image.getContentType() : "image/jpeg";

            response = imageBytes.length > 0
                    ? openAiClient.callVision(system, user, imageBytes, mimeType)
                    : openAiClient.callText(system, user);

            log.info("OpenAI PRODUCT_GENERATE responded in {}ms", response.getLatencyMs());
            ProductGenerateResponse result = objectMapper.readValue(response.getText(), ProductGenerateResponse.class);
            result.setLatencyMs(response.getLatencyMs());
            saveAiRequest(sellerId, "PRODUCT_GENERATE", "Generated for: " + request.getSellerIntent(), response, "SUCCESS");
            return result;

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            saveAiRequestSafe(sellerId, "PRODUCT_GENERATE", "Failed: " + request.getSellerIntent());
            log.error("Failed to parse ProductGenerateResponse. Raw: {}", response != null ? response.getText() : "<no response>", e);
            throw new AiParseException("Could not parse AI response", e);
        } catch (AiException e) {
            saveAiRequestSafe(sellerId, "PRODUCT_GENERATE", "Failed: " + request.getSellerIntent());
            throw e;
        } catch (Exception e) {
            saveAiRequestSafe(sellerId, "PRODUCT_GENERATE", "Failed: " + request.getSellerIntent());
            throw new RuntimeException(e);
        }
    }

    public PlatformSuggestResponse suggestPlatforms(Long sellerId, PlatformSuggestRequest request, MultipartFile image) {
        OpenAiResponse response = null;
        try {
            String system = PlatformSuggesterPrompt.system();
            String user = PlatformSuggesterPrompt.user(request.getProductName());

            byte[] imageBytes = image != null ? image.getBytes() : new byte[0];
            String mimeType = image != null && image.getContentType() != null ? image.getContentType() : "image/jpeg";

            response = imageBytes.length > 0
                    ? openAiClient.callVision(system, user, imageBytes, mimeType)
                    : openAiClient.callText(system, user);

            log.info("OpenAI PLATFORM_SUGGEST responded in {}ms", response.getLatencyMs());
            PlatformSuggestResponse result = objectMapper.readValue(response.getText(), PlatformSuggestResponse.class);
            result.setLatencyMs(response.getLatencyMs());
            saveAiRequest(sellerId, "PLATFORM_SUGGEST", "Suggested for: " + request.getProductName(), response, "SUCCESS");
            return result;

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            saveAiRequestSafe(sellerId, "PLATFORM_SUGGEST", "Failed for: " + request.getProductName());
            log.error("Failed to parse PlatformSuggestResponse. Raw: {}", response != null ? response.getText() : "<no response>", e);
            throw new AiParseException("Could not parse AI response", e);
        } catch (AiException e) {
            saveAiRequestSafe(sellerId, "PLATFORM_SUGGEST", "Failed for: " + request.getProductName());
            throw e;
        } catch (Exception e) {
            saveAiRequestSafe(sellerId, "PLATFORM_SUGGEST", "Failed for: " + request.getProductName());
            throw new RuntimeException(e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAiRequest(Long sellerId, String feature, String summary, OpenAiResponse response, String status) {
        AiRequest aiReq = AiRequest.builder()
                .sellerId(sellerId)
                .feature(feature)
                .inputSummary(summary.substring(0, Math.min(summary.length(), 250)))
                .status(status)
                .build();

        if (response != null) {
            aiReq.setGeminiModel("gpt-4o");
            aiReq.setPromptTokens(response.getPromptTokens());
            aiReq.setOutputTokens(response.getOutputTokens());
            aiReq.setLatencyMs((int) response.getLatencyMs());
        }

        aiRequestRepository.save(aiReq);
    }

    private void saveAiRequestSafe(Long sellerId, String feature, String summary) {
        try {
            saveAiRequest(sellerId, feature, summary, null, "FAILED");
        } catch (Exception ex) {
            log.warn("Could not save AI audit for {}: {}", feature, ex.getMessage());
        }
    }
}
