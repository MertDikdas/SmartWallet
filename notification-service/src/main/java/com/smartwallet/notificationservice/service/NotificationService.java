package com.smartwallet.notificationservice.service;

import com.smartwallet.notificationservice.dto.response.MarkAllNotificationsReadResponse;
import com.smartwallet.notificationservice.dto.response.NotificationResponse;
import com.smartwallet.notificationservice.dto.response.UnreadNotificationCountResponse;
import com.smartwallet.notificationservice.entity.Notification;
import com.smartwallet.notificationservice.exception.NotificationNotFoundException;
import com.smartwallet.notificationservice.mapper.NotificationMapper;
import com.smartwallet.notificationservice.repository.NotificationRepository;
import com.smartwallet.notificationservice.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(
            Long userId,
            boolean unreadOnly,
            int page,
            int size
    ) {
        @SuppressWarnings("SPRING_DATA_STRING_PROPERTY_REFERENCE")
        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        Page<Notification> notificationPage =
                unreadOnly
                        ? notificationRepository
                        .findAllByUserIdAndReadFalse(
                                userId,
                                pageable
                        )
                        : notificationRepository
                        .findAllByUserId(
                                userId,
                                pageable
                        );

        Page<NotificationResponse> responsePage =
                notificationPage.map(
                        notificationMapper::toResponse
                );

        return PageResponse.from(responsePage);
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