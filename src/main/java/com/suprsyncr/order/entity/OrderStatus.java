package com.suprsyncr.order.entity;

/**
 * Enum representing the lifecycle status of an order.
 * Defines valid states in the order state machine.
 */
public enum OrderStatus {
    PENDING,
    ACCEPTED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED
}

