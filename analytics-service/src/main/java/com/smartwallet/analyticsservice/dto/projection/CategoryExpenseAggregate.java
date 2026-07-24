package com.smartwallet.analyticsservice.dto.projection;

import java.math.BigDecimal;

public record CategoryExpenseAggregate(
        Long categoryId,
        BigDecimal totalExpense,
        Long transactionCount
) {
}
