package com.smartwallet.notificationservice.dto.response;

import com.smartwallet.notificationservice.entity.NotificationResourceType;
import com.smartwallet.notificationservice.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        NotificationResourceType resourceType,
        Long resourceId,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}