package com.smartwallet.notificationservice.repository;

import com.smartwallet.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByUserId(
            Long userId,
            Pageable pageable
    );
    Page<Notification> findAllByUserIdAndReadFalse(
            Long userId,
            Pageable pageable
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
                    resource_type,
                    resource_id,
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
                    'BUDGET',
                    :budgetId,
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
            @Param("sourceEventId") UUID sourceEventId
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query(
            value = """
                UPDATE notifications
                SET is_read = TRUE,
                    read_at = CURRENT_TIMESTAMP
                WHERE user_id = :userId
                  AND is_read = FALSE
                """,
            nativeQuery = true
    )
    int markAllAsReadByUserId(
            @Param("userId") Long userId
    );

    long countByUserIdAndReadFalse(Long userId);


    @Modifying
    @Query(
            value = """
            INSERT INTO notifications
            (
                user_id,
                type,
                title,
                message,
                resource_type,
                resource_id,
                source_event_id,
                is_read,
                created_at
            )
            VALUES
            (
                :userId,
                'RECURRING_TRANSACTION_FAILED',
                :title,
                :message,
                'RECURRING_TRANSACTION',
                :recurringTransactionId,
                :sourceEventId,
                FALSE,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (source_event_id) DO NOTHING
            """,
            nativeQuery = true
    )
    int insertRecurringTransactionFailedNotification(
            @Param("userId")
            Long userId,

            @Param("title")
            String title,

            @Param("message")
            String message,

            @Param("recurringTransactionId")
            Long recurringTransactionId,

            @Param("sourceEventId")
            UUID sourceEventId
    );
}