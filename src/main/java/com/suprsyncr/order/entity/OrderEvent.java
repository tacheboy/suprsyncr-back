package com.suprsyncr.order.entity;

import com.suprsyncr.common.entity.BaseEntity;
import jakarta.persistence.*;

/**
 * Entity representing an event in the order lifecycle.
 * Provides complete audit trail of all status transitions and actions.
 */
@Entity
@Table(name = "order_events")
public class OrderEvent extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 50)
    private OrderStatus fromStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 50)
    private OrderStatus toStatus;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false, length = 50)
    private EventTrigger triggeredBy;
    
    // Getters and Setters
    
    public Order getOrder() {
        return order;
    }
    
    public void setOrder(Order order) {
        this.order = order;
    }
    
    public OrderStatus getFromStatus() {
        return fromStatus;
    }
    
    public void setFromStatus(OrderStatus fromStatus) {
        this.fromStatus = fromStatus;
    }
    
    public OrderStatus getToStatus() {
        return toStatus;
    }
    
    public void setToStatus(OrderStatus toStatus) {
        this.toStatus = toStatus;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public EventTrigger getTriggeredBy() {
        return triggeredBy;
    }
    
    public void setTriggeredBy(EventTrigger triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
}

