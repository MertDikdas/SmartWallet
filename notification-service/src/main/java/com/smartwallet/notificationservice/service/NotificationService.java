package com.smartwallet.notificationservice.service;

import com.smartwallet.notificationservice.dto.response.MarkAllNotificationsReadResponse;
import com.smartwallet.notificationservice.dto.response.NotificationResponse;
import com.smartwallet.notificationservice.dto.response.UnreadNotificationCountResponse;
import com.smartwallet.notificationservice.entity.Notification;
import com.smartwallet.notificationservice.exception.NotificationNotFoundException;
import com.smartwallet.notificationservice.mapper.NotificationMapper;
import com.smartwallet.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(
            Long userId,
            boolean unreadOnly
    ) {
        List<Notification> notifications =
                unreadOnly
                        ? notificationRepository
                        .findAllByUserIdAndReadFalseOrderByCreatedAtDesc(
                                userId
                        )
                        : notificationRepository
                        .findAllByUserIdOrderByCreatedAtDesc(
                                userId
                        );

        return notifications.stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(
            Long userId,
            Long notificationId
    ) {
        return notificationMapper.toResponse(
                findOwnedNotification(
                        userId,
                        notificationId
                )
        );
    }

    @Transactional
    public NotificationResponse markAsRead(
            Long userId,
            Long notificationId
    ) {
        Notification notification =
                findOwnedNotification(
                        userId,
                        notificationId
                );

        notification.markAsRead();

        return notificationMapper.toResponse(
                notification
        );
    }

    private Notification findOwnedNotification(
            Long userId,
            Long notificationId
    ) {
        return notificationRepository
                .findByIdAndUserId(
                        notificationId,
                        userId
                )
                .orElseThrow(
                        () -> new NotificationNotFoundException(
                                notificationId
                        )
                );
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(
            Long userId
    ) {
        long unreadCount =
                notificationRepository
                        .countByUserIdAndReadFalse(userId);

        return new UnreadNotificationCountResponse(
                unreadCount
        );
    }

    @Transactional
    public MarkAllNotificationsReadResponse markAllAsRead(
            Long userId
    ) {
        int updatedCount =
                notificationRepository
                        .markAllAsReadByUserId(userId);

        return new MarkAllNotificationsReadResponse(
                updatedCount
        );
    }
}