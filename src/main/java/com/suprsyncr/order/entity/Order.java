package com.suprsyncr.order.entity;

import com.suprsyncr.common.entity.BaseEntity;
import com.suprsyncr.seller.entity.Seller;
import com.suprsyncr.seller.entity.SellerPlatform;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an order received from a marketplace platform.
 * Tracks the complete order lifecycle from ingestion to fulfillment.
 */
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    private SellerPlatform platform;
    
    @Column(nullable = false, unique = true)
    private String externalOrderId;
    
    @Column(nullable = false, unique = true)
    private String uspOrderId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(nullable = false)
    private String customerName;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;
    
    @Column(length = 20)
    private String customerPhone;
    
    @Column
    private String customerEmail;
    
    @Column(nullable = false)
    private LocalDateTime orderedAt;
    
    @Column
    private LocalDateTime acceptedAt;
    
    @Column
    private LocalDateTime shippedAt;
    
    @Column
    private LocalDateTime deliveredAt;
    
    @Column
    private LocalDateTime cancelledAt;
    
    @Column(columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Column
    private String trackingNumber;
    
    @Column
    private String courierPartner;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderEvent> events = new ArrayList<>();
    
    // Getters and Setters
    
    public Seller getSeller() {
        return seller;
    }
    
    public void setSeller(Seller seller) {
        this.seller = seller;
    }
    
    public SellerPlatform getPlatform() {
        return platform;
    }
    
    public void setPlatform(SellerPlatform platform) {
        this.platform = platform;
    }
    
    public String getExternalOrderId() {
        return externalOrderId;
    }
    
    public void setExternalOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
    }
    
    public String getUspOrderId() {
        return uspOrderId;
    }
    
    public void setUspOrderId(String uspOrderId) {
        this.uspOrderId = uspOrderId;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }
    
    public void setOrderedAt(LocalDateTime orderedAt) {
        this.orderedAt = orderedAt;
    }
    
    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }
    
    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
    
    public LocalDateTime getShippedAt() {
        return shippedAt;
    }
    
    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }
    
    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }
    
    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
    
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
    
    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public String getCourierPartner() {
        return courierPartner;
    }
    
    public void setCourierPartner(String courierPartner) {
        this.courierPartner = courierPartner;
    }
    
    public List<OrderItem> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    
    public List<OrderEvent> getEvents() {
        return events;
    }
    
    public void setEvents(List<OrderEvent> events) {
        this.events = events;
    }
}

