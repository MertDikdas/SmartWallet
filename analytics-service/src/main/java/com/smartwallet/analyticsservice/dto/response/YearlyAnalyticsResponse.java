package com.smartwallet.analyticsservice.dto.response;


import java.math.BigDecimal;

public record YearlyAnalyticsResponse(
        Integer year,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netAmount,
        Long transactionCount,
        BigDecimal averageMonthlyExpense
) {
}
