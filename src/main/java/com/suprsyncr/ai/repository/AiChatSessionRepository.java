package com.suprsyncr.ai.repository;

import com.suprsyncr.ai.domain.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSession, UUID> {
    List<AiChatSession> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    Optional<AiChatSession> findByIdAndSellerId(UUID id, Long sellerId);
}

