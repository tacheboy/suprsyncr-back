package com.suprsyncr.order.repository;

import com.suprsyncr.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for OrderItem entity.
 * Provides data access methods for order item management.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}

