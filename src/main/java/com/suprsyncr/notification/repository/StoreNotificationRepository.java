package com.suprsyncr.notification.repository;

import com.suprsyncr.notification.entity.StoreNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreNotificationRepository extends JpaRepository<StoreNotification, Long> {
    Page<StoreNotification> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);
    Page<StoreNotification> findBySellerIdAndReadAtIsNullOrderByCreatedAtDesc(Long sellerId, Pageable pageable);
    long countBySellerIdAndReadAtIsNull(Long sellerId);
    Optional<StoreNotification> findByIdAndSellerId(Long id, Long sellerId);
}
