package com.smartwallet.financeservice.dto.request;

import com.smartwallet.financeservice.entity.RecurrenceFrequency;
import com.smartwallet.financeservice.entity.TransactionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateRecurringTransactionRequest(

        @NotNull
        @Positive
        Long accountId,

        @NotNull
        @Positive
        Long categoryId,

        @NotNull
        TransactionType type,

        @NotNull
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Amount can have at most 2 decimal places"
        )
        BigDecimal amount,

        @Size(
                max = 255,
                message = "Description cannot exceed 255 characters"
        )
        String description,

        @NotNull
        RecurrenceFrequency frequency,

        @NotNull
        @FutureOrPresent(
                message = "Start date cannot be in the past"
        )
        LocalDate startDate,

        LocalDate endDate

) {

    @AssertTrue(
            message = "End date must be on or after start date"
    )
    public boolean isDateRangeValid() {
        return startDate == null
                || endDate == null
                || !endDate.isBefore(startDate);
    }
}