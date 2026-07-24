package com.smartwallet.analyticsservice.dto.projection;

import java.math.BigDecimal;

public record CategoryExpenseAggregate(
        Long categoryId,
        String categoryName,
        BigDecimal totalExpense,
        Long transactionCount
) {
}
