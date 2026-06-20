package com.suprsyncr.autopilot.repository;

import com.suprsyncr.autopilot.domain.ChangeImpact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChangeImpactRepository extends JpaRepository<ChangeImpact, UUID> {

    List<ChangeImpact> findByStoreId(String storeId);

    List<ChangeImpact> findByChangeId(UUID changeId);
}

