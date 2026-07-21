package com.smartwallet.financeservice.dto.response;

import com.smartwallet.financeservice.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        Long accountId,
        Long categoryId,
        String categoryName,
        TransactionType type,
        BigDecimal amount,
        String description,
        Instant transactionDate,
        Instant createdAt
) {
}