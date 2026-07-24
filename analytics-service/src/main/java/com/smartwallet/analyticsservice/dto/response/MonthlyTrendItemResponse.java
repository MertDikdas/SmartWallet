package com.smartwallet.analyticsservice.dto.response;

import java.math.BigDecimal;

public record MonthlyTrendItemResponse(
        Integer year,
        Integer month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netAmount,
        Long transactionCount
) {
}
