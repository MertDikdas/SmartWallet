package com.smartwallet.notificationservice.dto.response;

import com.smartwallet.notificationservice.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long budgetId,
        Long categoryId,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}