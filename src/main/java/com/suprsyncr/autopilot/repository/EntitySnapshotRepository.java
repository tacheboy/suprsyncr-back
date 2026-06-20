package com.suprsyncr.autopilot.repository;

import com.suprsyncr.autopilot.domain.EntitySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntitySnapshotRepository extends JpaRepository<EntitySnapshot, UUID> {

    Optional<EntitySnapshot> findByChangeId(UUID changeId);
}

