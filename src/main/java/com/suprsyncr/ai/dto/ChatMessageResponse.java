package com.suprsyncr.ai.dto;

import com.suprsyncr.ai.domain.AiChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private UUID messageId;
    private String role;
    private String content;
    private String createdAt;
    private Long latencyMs;

    public static ChatMessageResponse from(AiChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().toString() : null)
                .build();
    }
}

