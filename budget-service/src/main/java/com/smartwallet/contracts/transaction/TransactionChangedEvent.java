package com.smartwallet.contracts.transaction;

import java.time.Instant;
import java.util.UUID;

public record TransactionChangedEvent(
        UUID eventId,
        TransactionEventType eventType,
        Instant occurredAt,
        TransactionSnapshot before,
        TransactionSnapshot after
) {

    public static TransactionChangedEvent created(
            TransactionSnapshot after
    ) {
        return new TransactionChangedEvent(
                UUID.randomUUID(),
                TransactionEventType.CREATED,
                Instant.now(),
                null,
                after
        );
    }

    public static TransactionChangedEvent updated(
            TransactionSnapshot before,
            TransactionSnapshot after
    ) {
        return new TransactionChangedEvent(
                UUID.randomUUID(),
                TransactionEventType.UPDATED,
                Instant.now(),
                before,
                after
        );
    }

    public static TransactionChangedEvent deleted(
            TransactionSnapshot before
    ) {
        return new TransactionChangedEvent(
                UUID.randomUUID(),
                TransactionEventType.DELETED,
                Instant.now(),
                before,
                null
        );
    }

    public String routingKey() {
        return switch (eventType) {
            case CREATED ->
                    TransactionMessagingConstants.CREATED_ROUTING_KEY;

            case UPDATED ->
                    TransactionMessagingConstants.UPDATED_ROUTING_KEY;

            case DELETED ->
                    TransactionMessagingConstants.DELETED_ROUTING_KEY;
        };
    }
}