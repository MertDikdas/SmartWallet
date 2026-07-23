package com.smartwallet.notificationservice.controller;

import com.smartwallet.notificationservice.dto.response.MarkAllNotificationsReadResponse;
import com.smartwallet.notificationservice.dto.response.NotificationResponse;
import com.smartwallet.notificationservice.dto.response.UnreadNotificationCountResponse;
import com.smartwallet.notificationservice.dto.response.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import com.smartwallet.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>>
    getNotifications(
            @AuthenticationPrincipal Jwt jwt,

            @RequestParam(
                    defaultValue = "false"
            )
            boolean unreadOnly,

            @RequestParam(
                    defaultValue = "0"
            )
            @Min(
                    value = 0,
                    message = "Page cannot be negative"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            @Min(
                    value = 1,
                    message = "Page size must be at least 1"
            )
            @Max(
                    value = 100,
                    message = "Page size cannot exceed 100"
            )
            int size
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                notificationService.getNotifications(
                        userId,
                        unreadOnly,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse>
    getNotification(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long notificationId
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                notificationService.getNotification(
                        userId,
                        notificationId
                )
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse>
    markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long notificationId
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                notificationService.markAsRead(
                        userId,
                        notificationId
                )
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse>
    getUnreadCount(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                notificationService.getUnreadCount(userId)
        );
    }

    @PatchMapping("/read-all")
    public ResponseEntity<MarkAllNotificationsReadResponse>
    markAllAsRead(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                notificationService.markAllAsRead(userId)
        );
    }
}