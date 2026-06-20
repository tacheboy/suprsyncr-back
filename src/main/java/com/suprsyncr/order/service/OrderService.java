package com.suprsyncr.order.service;

import com.suprsyncr.integration.connector.ExternalOrder;
import com.suprsyncr.order.dto.*;
import com.suprsyncr.order.entity.OrderStatus;
import com.suprsyncr.seller.entity.PlatformType;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for order management operations.
 * Provides order lifecycle management with state machine enforcement.
 * 
 * Requirements: 14, 15, 16, 17, 18, 19, 20, 21, 79, 84, 95
 */
public interface OrderService {
    
    /**
     * Get a single order by ID.
     */
    OrderDto getOrder(Long orderId);
    
    /**
     * Get an order by its external order ID from the marketplace.
     */
    OrderDto getOrderByExternalId(String externalOrderId);
    
    /**
     * Get orders with filtering and pagination.
     */
    Page<OrderDto> getOrders(
        int page,
        int size,
        OrderStatus status,
        PlatformType platformType,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
    /**
     * Get order statistics with optional date range filtering.
     */
    OrderStatsDto getOrderStats(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Accept an order and reserve inventory.
     * Validates status is PENDING, reserves inventory for each item,
     * updates status to ACCEPTED, and notifies marketplace.
     */
    OrderDto acceptOrder(Long orderId, AcceptOrderRequest request);
    
    /**
     * Ship an order and commit inventory.
     * Validates status is ACCEPTED, updates status to SHIPPED,
     * commits inventory reservations, and notifies marketplace with tracking.
     */
    OrderDto shipOrder(Long orderId, ShipOrderRequest request);
    
    /**
     * Mark an order as delivered.
     * Validates status is SHIPPED and updates status to DELIVERED.
     */
    OrderDto markDelivered(Long orderId);
    
    /**
     * Cancel an order and release reserved inventory if applicable.
     * If status is PENDING, just updates to CANCELLED.
     * If status is ACCEPTED, releases reserved inventory.
     */
    OrderDto cancelOrder(Long orderId, CancelOrderRequest request);
    
    /**
     * Get event history for an order.
     */
    List<OrderEventDto> getOrderEvents(Long orderId);
    
    /**
     * Ingest an order from external marketplace.
     * Checks if order exists by externalOrderId, generates unique uspOrderId,
     * creates order with PENDING status, creates order items, and creates order event.
     */
    OrderDto ingestOrder(Long platformId, ExternalOrder externalOrder);
    
    /**
     * Sync order status from marketplace platform.
     * Used by scheduled polling to fetch and update orders.
     */
    void syncOrderStatus(Long platformId);
}

