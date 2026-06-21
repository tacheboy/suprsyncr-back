package com.suprsyncr.notification.repository;

import com.suprsyncr.notification.entity.ShopifyWebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopifyWebhookDeliveryRepository extends JpaRepository<ShopifyWebhookDelivery, Long> {
    boolean existsByDeliveryIdAndStatus(String deliveryId, String status);
    Optional<ShopifyWebhookDelivery> findByDeliveryId(String deliveryId);
}
