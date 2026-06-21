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

    /**
     * Changes applied to a specific entity (product) within a window. Used by
     * the attribution trigger gate and the get_change_history tool.
     */
    @Query("""
        SELECT pc FROM ProposedChangeEntity pc
        WHERE pc.status = 'APPLIED'
          AND pc.shopifyEntityId = :entityId
          AND pc.appliedAt >= :since
        ORDER BY pc.appliedAt DESC
        """)
    List<ProposedChangeEntity> findAppliedForEntitySince(
            @Param("entityId") String entityId,
            @Param("since") LocalDateTime since);

    /**
     * Approved (not necessarily applied) changes to a specific entity within
     * a window. The attribution trigger gate accepts either status to admit a
     * sale for analysis even when the seller approved but hasn't manually
     * applied yet.
     */
    @Query("""
        SELECT pc FROM ProposedChangeEntity pc
        WHERE pc.status IN ('APPROVED','APPLIED')
          AND pc.shopifyEntityId = :entityId
          AND COALESCE(pc.appliedAt, pc.approvedAt) >= :since
        ORDER BY COALESCE(pc.appliedAt, pc.approvedAt) DESC
        """)
    List<ProposedChangeEntity> findRecentApprovedForEntity(
            @Param("entityId") String entityId,
            @Param("since") LocalDateTime since);
}

