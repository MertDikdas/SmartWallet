package com.smartwallet.contracts.recurring;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringTransactionFailedEvent(
        UUID eventId,
        Instant occurredAt,
        Long recurringTransactionId,
        Long userId,
        LocalDate scheduledDate,
        Integer attemptCount,
        String errorMessage

) {
}
