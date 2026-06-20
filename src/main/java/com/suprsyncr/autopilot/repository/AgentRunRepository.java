package com.suprsyncr.autopilot.repository;

import com.suprsyncr.autopilot.domain.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {

    List<AgentRun> findByStoreIdOrderByTriggeredAtDesc(String storeId);

    List<AgentRun> findByStatus(String status);
}

