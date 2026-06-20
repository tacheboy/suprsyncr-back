package com.suprsyncr.autopilot.repository;

import com.suprsyncr.autopilot.domain.ProposedChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProposedChangeRepository extends JpaRepository<ProposedChangeEntity, UUID> {

    List<ProposedChangeEntity> findByRunId(UUID runId);

    List<ProposedChangeEntity> findByStoreIdAndStatus(String storeId, String status);

    List<ProposedChangeEntity> findByStoreIdOrderByChangeIdDesc(String storeId);

    /** Find changes applied ~7 days ago that need impact measurement. */
    @Query("""
        SELECT pc FROM ProposedChangeEntity pc
        WHERE pc.status = 'APPLIED'
          AND pc.appliedAt BETWEEN :from AND :to
        """)
    List<ProposedChangeEntity> findAppliedBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}

