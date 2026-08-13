package com.smartwallet.financeservice.exception;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class RecurringTransactionExecutionException
        extends RuntimeException {

    private final Long recurringTransactionId;
    private final LocalDate scheduledDate;

    public RecurringTransactionExecutionException(
            Long recurringTransactionId,
            LocalDate scheduledDate,
            Throwable cause
    ) {
        super(
                "Recurring transaction execution failed",
                cause
        );

        this.recurringTransactionId =
                recurringTransactionId;

        this.scheduledDate =
                scheduledDate;
    }
}