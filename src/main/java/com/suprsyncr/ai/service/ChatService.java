package com.suprsyncr.ai.service;

import com.suprsyncr.ai.client.OpenAiClient;
import com.suprsyncr.ai.context.SellerContextBuilder;
import com.suprsyncr.ai.domain.AiChatMessage;
import com.suprsyncr.ai.domain.AiChatSession;
import com.suprsyncr.ai.domain.AiRequest;
import com.suprsyncr.ai.dto.ChatMessageResponse;
import com.suprsyncr.ai.dto.ChatSessionResponse;
import com.suprsyncr.ai.prompt.ChatPrompt;
import com.suprsyncr.ai.repository.AiChatMessageRepository;
import com.suprsyncr.ai.repository.AiChatSessionRepository;
import com.suprsyncr.ai.repository.AiRequestRepository;
import com.suprsyncr.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiClient openAiClient;
    private final SellerContextBuilder contextBuilder;
    private final AiChatSessionRepository sessionRepo;
    private final AiChatMessageRepository messageRepo;
    private final AiRequestRepository aiRequestRepo;

    @Transactional
    public ChatSessionResponse startSession(Long sellerId, String sessionName) {
        AiChatSession session = AiChatSession.builder()
                .sellerId(sellerId)
                .sessionName(sessionName != null ? sessionName : "Business Chat")
                .build();
        session = sessionRepo.save(session);
        return ChatSessionResponse.from(session);
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long sellerId, UUID sessionId, String userMessage) {
        AiChatSession session = sessionRepo.findByIdAndSellerId(sessionId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));

        List<AiChatMessage> history = messageRepo.findTop10BySessionIdOrderByCreatedAtAsc(sessionId);

        // Convert to OpenAI message format
        List<Map<String, Object>> openAiHistory = history.stream()
                .map(m -> Map.<String, Object>of(
                        "role", m.getRole().equals("model") ? "assistant" : m.getRole(),
                        "content", m.getContent()))
                .collect(Collectors.toList());

        String systemPrompt = ChatPrompt.system(contextBuilder.buildRichContext(sellerId, 7));

        var response = openAiClient.callChat(systemPrompt, openAiHistory, userMessage);

        messageRepo.save(AiChatMessage.builder()
                .session(session).role("user").content(userMessage).build());
        AiChatMessage modelMsg = messageRepo.save(AiChatMessage.builder()
                .session(session).role("model").content(response.getText()).build());

        aiRequestRepo.save(AiRequest.builder()
                .sellerId(sellerId).feature("CHAT")
                .geminiModel("gpt-4o")
                .promptTokens(response.getPromptTokens())
                .outputTokens(response.getOutputTokens())
                .latencyMs((int) response.getLatencyMs())
                .status("SUCCESS").build());

        return ChatMessageResponse.builder()
                .messageId(modelMsg.getId())
                .content(response.getText())
                .latencyMs(response.getLatencyMs())
                .build();
    }

    public List<ChatMessageResponse> getHistory(Long sellerId, UUID sessionId) {
        sessionRepo.findByIdAndSellerId(sessionId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(ChatMessageResponse::from).collect(Collectors.toList());
    }

    public List<ChatSessionResponse> getSessions(Long sellerId) {
        return sessionRepo.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream().map(ChatSessionResponse::from).collect(Collectors.toList());
    }
}
