package com.suprsyncr.notification.entity;

import com.suprsyncr.common.entity.BaseEntity;
import com.suprsyncr.seller.entity.Seller;
import com.suprsyncr.seller.entity.SellerPlatform;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "store_notifications")
public class StoreNotification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    private SellerPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String webhookTopic;

    @Column(length = 255)
    private String externalOrderId;

    @Column(length = 255)
    private String customerName;

    @Column(precision = 12, scale = 2)
    private BigDecimal orderTotal;

    @Column(length = 10)
    private String currency;

    @Column(length = 100)
    private String paymentStatus;

    private LocalDateTime readAt;

    public Seller getSeller() { return seller; }
    public void setSeller(Seller seller) { this.seller = seller; }
    public SellerPlatform getPlatform() { return platform; }
    public void setPlatform(SellerPlatform platform) { this.platform = platform; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getWebhookTopic() { return webhookTopic; }
    public void setWebhookTopic(String webhookTopic) { this.webhookTopic = webhookTopic; }
    public String getExternalOrderId() { return externalOrderId; }
    public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getOrderTotal() { return orderTotal; }
    public void setOrderTotal(BigDecimal orderTotal) { this.orderTotal = orderTotal; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
