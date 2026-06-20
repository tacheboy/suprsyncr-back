package com.suprsyncr.autopilot.repository;

import com.suprsyncr.autopilot.domain.AgentTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentTaskRepository extends JpaRepository<AgentTaskEntity, UUID> {
    List<AgentTaskEntity> findByRunId(UUID runId);
}
