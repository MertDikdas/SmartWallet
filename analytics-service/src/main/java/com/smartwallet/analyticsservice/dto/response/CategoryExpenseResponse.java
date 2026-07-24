package com.smartwallet.analyticsservice.dto.response;

import java.math.BigDecimal;

public record CategoryExpenseResponse(
        Long categoryId,
        String categoryName,
        BigDecimal totalExpense,
        BigDecimal percentage,
        Long transactionCount
) {
}
