package com.suprsyncr.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.ai.dto.*;
import com.suprsyncr.ai.service.ChatService;
import com.suprsyncr.ai.service.ContentOptimizerService;
import com.suprsyncr.ai.service.InsightsService;
import com.suprsyncr.auth.service.AuthService;
import com.suprsyncr.common.dto.ApiResponse;
import com.suprsyncr.common.exception.ResourceNotFoundException;
import com.suprsyncr.seller.entity.Seller;
import com.suprsyncr.seller.repository.SellerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI Tools and Copilot endpoints")
public class AiController {

    private final ContentOptimizerService contentOptimizerService;
    private final InsightsService insightsService;
    private final ChatService chatService;
    private final AuthService authService;
    private final SellerRepository sellerRepository;
    private final ObjectMapper objectMapper;

    // --- Content Optimization ---

    @PostMapping(value = "/content/optimize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Optimize Content", description = "Optimizes existing product listing content for marketplaces")
    public ResponseEntity<ApiResponse<ContentOptimizeResponse>> optimizeContent(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "image", required = false) MultipartFile image) throws JsonProcessingException {
        
        Long sellerId = getCurrentSellerId();
        ContentOptimizeRequest request = objectMapper.readValue(requestJson, ContentOptimizeRequest.class);
        ContentOptimizeResponse response = contentOptimizerService.optimizeForAmazon(sellerId, request, image);
        
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                response,
                "Content optimized successfully",
                LocalDateTime.now()
        ));
    }

    @PostMapping(value = "/content/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Generate Product", description = "Generates product listing from single image and seller intent")
    public ResponseEntity<ApiResponse<ProductGenerateResponse>> generateProduct(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "image", required = false) MultipartFile image) throws JsonProcessingException {
        
        Long sellerId = getCurrentSellerId();
        ProductGenerateRequest request = objectMapper.readValue(requestJson, ProductGenerateRequest.class);
        ProductGenerateResponse response = contentOptimizerService.generateProduct(sellerId, request, image);
        
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                response,
                "Product generated successfully",
                LocalDateTime.now()
        ));
    }

    // --- Insights ---

    @GetMapping("/insights/weekly")
    @Operation(summary = "Weekly Insights", description = "Get weekly business insights powered by AI")
    public ResponseEntity<ApiResponse<InsightResponse>> getWeeklyInsights(
            @RequestParam(required = false, defaultValue = "false") boolean forceRefresh) {
        Long sellerId = getCurrentSellerId();
        InsightResponse response = insightsService.getWeeklyInsights(sellerId, forceRefresh);
        
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                response,
                "Weekly insights generated successfully",
                LocalDateTime.now()
        ));
    }

    @GetMapping("/insights/monthly")
    @Operation(summary = "Monthly Insights", description = "Get monthly business insights powered by AI")
    public ResponseEntity<ApiResponse<InsightResponse>> getMonthlyInsights(
            @RequestParam(required = false, defaultValue = "false") boolean forceRefresh) {
        Long sellerId = getCurrentSellerId();
        InsightResponse response = insightsService.getMonthlyInsights(sellerId, forceRefresh);
        
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                response,
                "Monthly insights generated successfully",
                LocalDateTime.now()
        ));
    }

    // --- Platform Suggester ---

    @PostMapping(value = "/platform/suggest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Suggest Platforms", description = "Analyzes a product image and name to recommend the best e-commerce platforms for maximum profit")
    public ResponseEntity<ApiResponse<PlatformSuggestResponse>> suggestPlatforms(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "image", required = false) MultipartFile image) throws JsonProcessingException {

        Long sellerId = getCurrentSellerId();
        PlatformSuggestRequest request = objectMapper.readValue(requestJson, PlatformSuggestRequest.class);
        PlatformSuggestResponse response = contentOptimizerService.suggestPlatforms(sellerId, request, image);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                response,
                "Platform suggestions generated successfully",
                LocalDateTime.now()
        ));
    }

    // --- Chat ---

    @PostMapping("/chat/sessions")
    @Operation(summary = "Start Chat Session", description = "Initialize a new AI chat context")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> startChatSession(
            @RequestBody(required = false) ChatSessionRequest request) {
        Long sellerId = getCurrentSellerId();
        String sessionName = (request != null && request.getSessionName() != null) ? request.getSessionName() : "Business Chat";
        ChatSessionResponse response = chatService.startSession(sellerId, sessionName);
        
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                response,
                "Chat session started",
                LocalDateTime.now()
        ));
    }

    @GetMapping("/chat/sessions")
    @Operation(summary = "List Chat Sessions", description = "Fetch all previous AI chat sessions")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getChatSessions() {
        Long sellerId = getCurrentSellerId();
        List<ChatSessionResponse> response = chatService.getSessions(sellerId);
        
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                response,
                "Chat sessions retrieved",
                LocalDateTime.now()
        ));
    }

    @PostMapping("/chat/sessions/{sessionId}/messages")
    @Operation(summary = "Send Chat Message", description = "Send a message to the AI copilot")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendChatMessage(
            @PathVariable UUID sessionId, @RequestBody ChatMessageRequest request) {
        Long sellerId = getCurrentSellerId();
        ChatMessageResponse response = chatService.sendMessage(sellerId, sessionId, request.getMessage());
        
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                response,
                "Message sent",
                LocalDateTime.now()
        ));
    }

    @GetMapping("/chat/sessions/{sessionId}/messages")
    @Operation(summary = "Get Chat History", description = "Fetch all messages in a specific chat session")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getChatHistory(
            @PathVariable UUID sessionId) {
        Long sellerId = getCurrentSellerId();
        List<ChatMessageResponse> response = chatService.getHistory(sellerId, sessionId);
        
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                response,
                "Chat history retrieved",
                LocalDateTime.now()
        ));
    }

    // --- Helpers ---

    private Long getCurrentSellerId() {
        Long userId = authService.getCurrentUser().getId();
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        return seller.getId();
    }

    private static class ChatSessionRequest {
        private String sessionName;
        public String getSessionName() { return sessionName; }
        public void setSessionName(String sessionName) { this.sessionName = sessionName; }
    }
}

