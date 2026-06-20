package com.suprsyncr.ai.repository;

import com.suprsyncr.ai.domain.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiRequestRepository extends JpaRepository<AiRequest, UUID> {
}

