package com.suprsyncr.ai.dto;

import com.suprsyncr.ai.domain.AiChatSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionResponse {
    private UUID sessionId;
    private String sessionName;
    private String createdAt;

    public static ChatSessionResponse from(AiChatSession session) {
        return ChatSessionResponse.builder()
                .sessionId(session.getId())
                .sessionName(session.getSessionName())
                .createdAt(session.getCreatedAt() != null ? session.getCreatedAt().toString() : null)
                .build();
    }
}

