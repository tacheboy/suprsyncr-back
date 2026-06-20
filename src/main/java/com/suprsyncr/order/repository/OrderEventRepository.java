package com.suprsyncr.order.repository;

import com.suprsyncr.order.entity.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for OrderEvent entity.
 * Provides data access methods for order event history tracking.
 */
@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    
    /**
     * Find all events for a specific order, ordered by creation time in ascending order.
     * This provides a chronological audit trail of all status changes and actions.
     * 
     * @param orderId the order ID
     * @return List of order events in chronological order
     */
    List<OrderEvent> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}

