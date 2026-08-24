package com.smartwallet.budgetservice.dto.response;

import com.smartwallet.budgetservice.entity.BudgetStatus;
import com.smartwallet.budgetservice.entity.CurrencyCode;

import java.math.BigDecimal;
import java.time.Instant;

public record BudgetResponse(
        Long id,
        Long categoryId,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        Integer year,
        Integer month,
        BudgetStatus status,
        CurrencyCode currency,
        Instant createdAt,
        Instant updatedAt
) {
}