package com.smartwallet.financeservice.dto.response;

import com.smartwallet.financeservice.entity.RecurringExecutionStatus;

import java.time.Instant;
import java.time.LocalDate;

public record RecurringTransactionExecutionResponse(

        Long id,

        LocalDate scheduledDate,

        RecurringExecutionStatus status,

        Long generatedTransactionId,

        String errorMessage,

        Instant createdAt,

        Instant completedAt

) {
}