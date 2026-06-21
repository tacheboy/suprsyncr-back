package com.suprsyncr.notification.entity;

import com.suprsyncr.common.entity.BaseEntity;
import com.suprsyncr.seller.entity.SellerPlatform;
import jakarta.persistence.*;

@Entity
@Table(name = "shopify_webhook_deliveries")
public class ShopifyWebhookDelivery extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id")
    private SellerPlatform platform;

    @Column(name = "delivery_id", unique = true, length = 255)
    private String deliveryId;

    @Column(length = 100)
    private String topic;

    @Column(name = "shop_domain", length = 255)
    private String shopDomain;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    public SellerPlatform getPlatform() { return platform; }
    public void setPlatform(SellerPlatform platform) { this.platform = platform; }
    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getShopDomain() { return shopDomain; }
    public void setShopDomain(String shopDomain) { this.shopDomain = shopDomain; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
}
