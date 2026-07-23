package com.smartwallet.notificationservice.repository;

import com.smartwallet.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query(
            value = """
                INSERT INTO notifications
                (
                    user_id,
                    type,
                    title,
                    message,
                    budget_id,
                    category_id,
                    source_event_id,
                    is_read,
                    created_at
                )
                VALUES
                (
                    :userId,
                    'BUDGET_EXCEEDED',
                    :title,
                    :message,
                    :budgetId,
                    :categoryId,
                    :sourceEventId,
                    FALSE,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (source_event_id) DO NOTHING
                """,
            nativeQuery = true
    )
    int insertBudgetExceededNotification(
            @Param("userId") Long userId,
            @Param("title") String title,
            @Param("message") String message,
            @Param("budgetId") Long budgetId,
            @Param("categoryId") Long categoryId,
            @Param("sourceEventId") UUID sourceEventId
    );
}