package com.suprsyncr.autopilot.repository;

import com.suprsyncr.autopilot.domain.ModelInvocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModelInvocationRepository extends JpaRepository<ModelInvocationEntity, UUID> {
    List<ModelInvocationEntity> findByRunId(UUID runId);
}
