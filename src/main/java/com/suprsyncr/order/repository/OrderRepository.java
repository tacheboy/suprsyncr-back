package com.suprsyncr.order.repository;

import com.suprsyncr.order.entity.Order;
import com.suprsyncr.order.entity.OrderStatus;
import com.suprsyncr.seller.entity.PlatformType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for Order entity.
 * Provides data access methods for order management with complex filtering and statistics.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find an order by its external order ID from the marketplace platform.
     * 
     * @param externalOrderId the external order ID from the marketplace
     * @return Optional containing the order if found
     */
    Optional<Order> findByExternalOrderId(String externalOrderId);
    
    /**
     * Find orders with complex filtering by seller, status, platform type, and date range.
     * Supports pagination for large result sets.
     * 
     * @param sellerId the seller ID
     * @param status the order status
     * @param platformType the platform type
     * @param start the start of the date range
     * @param end the end of the date range
    /**
     * @param pageable pagination parameters
     * @return Page of orders matching the criteria
     */
//    Page<Order> findBySellerIdAndStatusAndPlatformPlatformTypeAndOrderedAtBetween(
//        Long sellerId,
//        OrderStatus status,
//        PlatformType platformType,
//        LocalDateTime start,
//        LocalDateTime end,
//        Pageable pageable
//    );
    /**
     * All filters (status + platform). Callers must pass non-null date bounds;
     * use wide defaults (epoch / far future) at the service layer for "no filter".
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.seller.id = :sellerId
        AND o.status = :status
        AND o.platform.platformType = :platformType
        AND o.orderedAt >= :start AND o.orderedAt <= :end
        ORDER BY o.orderedAt DESC
        """)
    Page<Order> findBySellerStatusPlatform(
        @Param("sellerId") Long sellerId,
        @Param("status") OrderStatus status,
        @Param("platformType") PlatformType platformType,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    /**
     * Status filter only.
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.seller.id = :sellerId
        AND o.status = :status
        AND o.orderedAt >= :start AND o.orderedAt <= :end
        ORDER BY o.orderedAt DESC
        """)
    Page<Order> findBySellerAndStatus(
        @Param("sellerId") Long sellerId,
        @Param("status") OrderStatus status,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    /**
     * Platform filter only.
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.seller.id = :sellerId
        AND o.platform.platformType = :platformType
        AND o.orderedAt >= :start AND o.orderedAt <= :end
        ORDER BY o.orderedAt DESC
        """)
    Page<Order> findBySellerAndPlatform(
        @Param("sellerId") Long sellerId,
        @Param("platformType") PlatformType platformType,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    /**
     * No status/platform filter â€” returns all orders for seller in date range.
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.seller.id = :sellerId
        AND o.orderedAt >= :start AND o.orderedAt <= :end
        ORDER BY o.orderedAt DESC
        """)
    Page<Order> findBySeller(
        @Param("sellerId") Long sellerId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    // Kept for backwards compat â€” delegates ignored now
    default Page<Order> findBySellerIdAndStatusAndPlatformPlatformTypeAndOrderedAtBetween(
        Long sellerId, OrderStatus status, PlatformType platformType,
        LocalDateTime start, LocalDateTime end, Pageable pageable
    ) {
        throw new UnsupportedOperationException("Use specific query methods instead");
    }
    
    /**
     * Calculate order statistics with aggregations.
     * Returns total order count, order counts by status, and total revenue from delivered orders.
     * 
     * @param sellerId the seller ID
     * @param start the start of the date range (optional)
     * @param end the end of the date range (optional)
     * @return Map containing statistics: totalOrders, pendingOrders, acceptedOrders, 
     *         shippedOrders, deliveredOrders, cancelledOrders, returnedOrders, totalRevenue
     */
    /**
     * Eagerly load an order with all associations the attribution pipeline
     * needs: items → productVariant → product, and platform. Avoids
     * LazyInitializationException when AttributionService accesses these
     * fields outside an open transaction.
     */
    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.items oi
        LEFT JOIN FETCH oi.productVariant pv
        LEFT JOIN FETCH pv.product
        LEFT JOIN FETCH o.platform
        WHERE o.id = :id
        """)
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    /**
     * Recent orders across all sellers, newest first, capped by Pageable.
     * Used by the Scenario 3 attribution poller to find orders that may
     * need attribution attempts. Cheap because Order is small per row and
     * the index on orderedAt is hit directly.
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.orderedAt >= :since
        ORDER BY o.orderedAt DESC
        """)
    java.util.List<com.suprsyncr.order.entity.Order> findRecentOrders(
            @Param("since") LocalDateTime since,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
        SELECT new map(
            COUNT(o) as totalOrders,
            SUM(CASE WHEN o.status = 'PENDING' THEN 1 ELSE 0 END) as pendingOrders,
            SUM(CASE WHEN o.status = 'ACCEPTED' THEN 1 ELSE 0 END) as acceptedOrders,
            SUM(CASE WHEN o.status = 'SHIPPED' THEN 1 ELSE 0 END) as shippedOrders,
            SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) as deliveredOrders,
            SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledOrders,
            SUM(CASE WHEN o.status = 'RETURNED' THEN 1 ELSE 0 END) as returnedOrders,
            COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.totalAmount ELSE 0 END), 0) as totalRevenue
        )
        FROM Order o
        WHERE o.seller.id = :sellerId
        AND (cast(:start as timestamp) IS NULL OR o.orderedAt >= cast(:start as timestamp))
        AND (cast(:end as timestamp) IS NULL OR o.orderedAt <= cast(:end as timestamp))
        """)
    Map<String, Object> calculateOrderStatistics(
        @Param("sellerId") Long sellerId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}

