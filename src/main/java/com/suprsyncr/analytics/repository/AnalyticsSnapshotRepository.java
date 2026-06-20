package com.suprsyncr.analytics.repository;

import com.suprsyncr.analytics.domain.AnalyticsSnapshot;
import com.suprsyncr.analytics.domain.AnalyticsSnapshot.SnapshotType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, Long> {

    /**
     * Get the most recent snapshot for a store + type.
     * Call with: PageRequest.of(0, 1) to get the single latest result.
     * JPQL does not support LIMIT â€” use Pageable instead.
     */
    @Query("""
        SELECT s FROM AnalyticsSnapshot s
        WHERE s.storeId = :storeId AND s.type = :type
        ORDER BY s.computedAt DESC
        """)
    List<AnalyticsSnapshot> findLatestForStore(
            @Param("storeId") String storeId,
            @Param("type") SnapshotType type,
            Pageable pageable);

    /**
     * Convenience wrapper â€” returns Optional of most-recent snapshot.
     * Used by AnalyticsOrchestrator.
     */
    default Optional<AnalyticsSnapshot> findLatest(String storeId, SnapshotType type) {
        List<AnalyticsSnapshot> results = findLatestForStore(
                storeId, type, org.springframework.data.domain.PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}

