package com.smartwallet.contracts.transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionSnapshot(
        Long transactionId,
        Long userId,
        Long accountId,
        Long categoryId,
        String categoryName,
        String transactionType,
        BigDecimal amount,
        CurrencyCode currency,
        Instant transactionDate
) {
}