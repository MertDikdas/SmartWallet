package com.smartwallet.financeservice.dto.request;

import com.smartwallet.financeservice.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateTransactionRequest(

        Long accountId,

        Long categoryId,

        TransactionType type,

        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        BigDecimal amount,

        @Size(
                max = 255,
                message = "Description cannot exceed 255 characters"
        )
        String description,

        Instant transactionDate

) {
}