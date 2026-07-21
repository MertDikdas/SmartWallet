package com.smartwallet.financeservice.dto.response;

import com.smartwallet.financeservice.entity.AccountType;
import com.smartwallet.financeservice.entity.CurrencyCode;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String name,
        AccountType type,
        BigDecimal balance,
        CurrencyCode currency,
        Instant createdAt,
        Instant updatedAt
) {
}