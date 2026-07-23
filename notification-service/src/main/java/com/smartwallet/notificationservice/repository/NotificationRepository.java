package com.smartwallet.notificationservice.repository;

import com.smartwallet.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification>
    findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification>
    findAllByUserIdAndReadFalseOrderByCreatedAtDesc(
            Long userId
    );

    Optional<Notification> findByIdAndUserId(
            Long notificationId,
            Long userId
    );

    boolean existsBySourceEventId(UUID sourceEventId);
}