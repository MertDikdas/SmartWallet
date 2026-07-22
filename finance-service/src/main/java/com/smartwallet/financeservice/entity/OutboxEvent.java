package com.smartwallet.financeservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    private static final int MAX_ATTEMPTS = 10;

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "routing_key", nullable = false, length = 100)
    private String routingKey;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Lob
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (status == null) {
            status = OutboxEventStatus.PENDING;
        }

        if (attemptCount == null) {
            attemptCount = 0;
        }

        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }
    }

    public void markPublished() {
        status = OutboxEventStatus.PUBLISHED;
        publishedAt = Instant.now();
        lastError = null;
    }

    public void markFailed(Exception exception) {
        attemptCount++;

        String message = exception.getMessage();

        lastError = message != null
                ? message.substring(0, Math.min(message.length(), 2000))
                : exception.getClass().getSimpleName();

        if (attemptCount >= MAX_ATTEMPTS) {
            status = OutboxEventStatus.FAILED;
            return;
        }

        status = OutboxEventStatus.PENDING;

        long retryDelaySeconds =
                Math.min(300, attemptCount * 10L);

        nextAttemptAt =
                Instant.now().plusSeconds(retryDelaySeconds);
    }
}