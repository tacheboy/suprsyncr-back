package com.suprsyncr.notification.dto;

import com.suprsyncr.notification.entity.NotificationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StoreNotificationDto(
        Long id,
        NotificationType type,
        String webhookTopic,
        String externalOrderId,
        String customerName,
        BigDecimal orderTotal,
        String currency,
        String paymentStatus,
        Long platformId,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {}
