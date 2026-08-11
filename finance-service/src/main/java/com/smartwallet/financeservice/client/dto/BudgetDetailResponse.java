package com.smartwallet.financeservice.client.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BudgetDetailResponse(
        Long id,
        Long categoryId,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        Integer year,
        Integer month,
        BudgetStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
