package com.smartwallet.financeservice.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateTransferRequest(

        @NotNull(message = "Source account ID is required")
        @Positive(message = "Source account ID must be positive")
        Long fromAccountId,

        @NotNull(message = "Destination account ID is required")
        @Positive(message = "Destination account ID must be positive")
        Long toAccountId,

        @NotNull(message = "Transfer amount is required")
        @Positive(message = "Transfer amount must be greater than zero")
        BigDecimal amount,

        @Size(
                max = 255,
                message = "Description cannot exceed 255 characters"
        )
        String description,

        Instant transferredAt

) {

    @AssertTrue(
            message = "Source and destination accounts must be different"
    )
    public boolean isAccountSelectionValid() {
        return fromAccountId == null
                || toAccountId == null
                || !fromAccountId.equals(toAccountId);
    }
}