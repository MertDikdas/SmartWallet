package com.smartwallet.contracts.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetExceededEvent(
        UUID eventId,
        Instant occuredAt,
        Long budgetId,
        Long userId,
        Long categoryId,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        Integer year,
        Integer month
) {
}
