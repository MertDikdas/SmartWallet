package com.smartwallet.financeservice.dto.response;

import com.smartwallet.financeservice.entity.RecurrenceFrequency;
import com.smartwallet.financeservice.entity.RecurringTransactionStatus;
import com.smartwallet.financeservice.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RecurringTransactionResponse(

        Long id,

        Long accountId,

        String accountName,

        Long categoryId,

        String categoryName,

        TransactionType type,

        BigDecimal amount,

        String description,

        RecurrenceFrequency frequency,

        RecurringTransactionStatus status,

        LocalDate startDate,

        LocalDate endDate,

        LocalDate nextExecutionDate,

        LocalDate lastExecutionDate,

        Instant createdAt,

        Instant updatedAt

) {
}