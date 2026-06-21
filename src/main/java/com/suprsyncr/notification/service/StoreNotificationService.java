package com.suprsyncr.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.suprsyncr.notification.dto.StoreNotificationDto;
import com.suprsyncr.notification.entity.NotificationType;
import com.suprsyncr.notification.entity.ShopifyWebhookDelivery;
import com.suprsyncr.notification.entity.StoreNotification;
import com.suprsyncr.notification.repository.ShopifyWebhookDeliveryRepository;
import com.suprsyncr.notification.repository.StoreNotificationRepository;
import com.suprsyncr.seller.entity.SellerPlatform;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StoreNotificationService {

    private final StoreNotificationRepository notificationRepository;
    private final ShopifyWebhookDeliveryRepository deliveryRepository;

    public StoreNotificationService(StoreNotificationRepository notificationRepository,
                                    ShopifyWebhookDeliveryRepository deliveryRepository) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public boolean hasProcessedDelivery(String deliveryId) {
        return deliveryId != null && !deliveryId.isBlank()
                && deliveryRepository.existsByDeliveryIdAndStatus(deliveryId, "PROCESSED");
    }

    @Transactional
    public StoreNotification recordShopifyOrderActivity(SellerPlatform platform, String topic,
                                                          String deliveryId, JsonNode order, String rawPayload) {
        NotificationType type = notificationTypeFor(topic);
        StoreNotification notification = new StoreNotification();
        notification.setSeller(platform.getSeller());
        notification.setPlatform(platform);
        notification.setType(type);
        notification.setWebhookTopic(topic);
        notification.setExternalOrderId(text(order, "id"));
        notification.setCustomerName(customerName(order));
        notification.setOrderTotal(decimal(order.path("total_price")));
        notification.setCurrency(text(order, "currency"));
        notification.setPaymentStatus(paymentStatus(order));
        StoreNotification saved = notificationRepository.save(notification);

        recordDelivery(platform, topic, deliveryId, text(order, "shop_domain"), "PROCESSED", null, rawPayload);
        return saved;
    }

    @Transactional
    public StoreNotification createInternalNotification(SellerPlatform platform, NotificationType type,
                                                         String syntheticTopic, String externalOrderId,
                                                         String customerName, BigDecimal amount, String currency) {
        StoreNotification n = new StoreNotification();
        n.setSeller(platform.getSeller());
        n.setPlatform(platform);
        n.setType(type);
        n.setWebhookTopic(syntheticTopic);
        n.setExternalOrderId(externalOrderId);
        n.setCustomerName(customerName);
        n.setOrderTotal(amount);
        n.setCurrency(currency != null ? currency : "INR");
        return notificationRepository.save(n);
    }

    @Transactional
    public void recordFailedDelivery(SellerPlatform platform, String topic, String deliveryId,
                                     String shopDomain, String rawPayload, Exception error) {
        if (hasProcessedDelivery(deliveryId)) return;
        recordDelivery(platform, topic, deliveryId, shopDomain, "FAILED", error.getMessage(), rawPayload);
    }

    public List<StoreNotificationDto> getNotifications(Long sellerId, boolean unreadOnly, int limit) {
        PageRequest page = PageRequest.of(0, Math.min(Math.max(limit, 1), 100));
        return (unreadOnly
                ? notificationRepository.findBySellerIdAndReadAtIsNullOrderByCreatedAtDesc(sellerId, page)
                : notificationRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, page))
                .map(this::toDto).getContent();
    }

    public long unreadCount(Long sellerId) {
        return notificationRepository.countBySellerIdAndReadAtIsNull(sellerId);
    }

    @Transactional
    public StoreNotificationDto markRead(Long sellerId, Long notificationId) {
        StoreNotification notification = notificationRepository.findByIdAndSellerId(notificationId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (notification.getReadAt() == null) notification.setReadAt(LocalDateTime.now());
        return toDto(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(Long sellerId) {
        List<StoreNotification> notifications = notificationRepository
                .findBySellerIdAndReadAtIsNullOrderByCreatedAtDesc(sellerId, PageRequest.of(0, 100)).getContent();
        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(notification -> notification.setReadAt(now));
        notificationRepository.saveAll(notifications);
    }

    private void recordDelivery(SellerPlatform platform, String topic, String deliveryId, String shopDomain,
                                String status, String errorMessage, String rawPayload) {
        ShopifyWebhookDelivery delivery = blankToNull(deliveryId) == null
                ? new ShopifyWebhookDelivery()
                : deliveryRepository.findByDeliveryId(deliveryId).orElseGet(ShopifyWebhookDelivery::new);
        delivery.setPlatform(platform);
        delivery.setDeliveryId(blankToNull(deliveryId));
        delivery.setTopic(topic);
        delivery.setShopDomain(shopDomain);
        delivery.setStatus(status);
        delivery.setErrorMessage(errorMessage);
        delivery.setRawPayload(rawPayload);
        deliveryRepository.save(delivery);
    }

    private NotificationType notificationTypeFor(String topic) {
        return switch (topic) {
            case "orders/updated" -> NotificationType.ORDER_UPDATED;
            case "orders/cancelled" -> NotificationType.ORDER_CANCELLED;
            case "orders/paid" -> NotificationType.PAYMENT_SUCCESSFUL;
            case "orders/fulfilled" -> NotificationType.ORDER_FULFILLED;
            default -> NotificationType.NEW_ORDER;
        };
    }

    private StoreNotificationDto toDto(StoreNotification n) {
        return new StoreNotificationDto(n.getId(), n.getType(), n.getWebhookTopic(), n.getExternalOrderId(),
                n.getCustomerName(), n.getOrderTotal(), n.getCurrency(), n.getPaymentStatus(),
                n.getPlatform().getId(), n.getReadAt() != null, n.getCreatedAt(), n.getReadAt());
    }

    private static String customerName(JsonNode order) {
        JsonNode customer = order.path("customer");
        String name = (customer.path("first_name").asText("") + " " + customer.path("last_name").asText("")).trim();
        return name.isBlank() ? customer.path("email").asText("") : name;
    }

    private static String paymentStatus(JsonNode order) {
        String financial = text(order, "financial_status");
        return financial == null || financial.isBlank() ? text(order, "display_financial_status") : financial;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static BigDecimal decimal(JsonNode node) {
        try { return node == null || node.isMissingNode() || node.isNull() ? null : new BigDecimal(node.asText()); }
        catch (NumberFormatException e) { return null; }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
