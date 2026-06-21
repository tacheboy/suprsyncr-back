package com.suprsyncr.autopilot.attribution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttributionRepository extends JpaRepository<Attribution, UUID> {

    Optional<Attribution> findByOrderId(Long orderId);

    /** Impact Lab list — newest first, all attempts (not just ATTRIBUTED). */
    List<Attribution> findByStoreIdOrderByTriggeredAtDesc(String storeId);

    /**
     * Order ids the poller has already touched. Postgres-only window is fine
     * for MVP; the poller passes a recent {@code orderIds} slice so this
     * query is bounded.
     */
    @Query("SELECT a.orderId FROM Attribution a WHERE a.orderId IN :orderIds")
    List<Long> findExistingOrderIds(@Param("orderIds") List<Long> orderIds);

    /** Used by ChangeImpact updater to credit realised revenue back to the
     * causal change after attribution lands. */
    List<Attribution> findByCausalChangeId(UUID causalChangeId);
}
