package com.suprsyncr.ai.repository;

import com.suprsyncr.ai.domain.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, UUID> {
    List<AiChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
    List<AiChatMessage> findTop10BySessionIdOrderByCreatedAtAsc(UUID sessionId);
}

