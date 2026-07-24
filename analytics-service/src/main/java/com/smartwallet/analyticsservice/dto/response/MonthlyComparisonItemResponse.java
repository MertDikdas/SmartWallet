package com.smartwallet.analyticsservice.dto.response;

import java.math.BigDecimal;

public record MonthlyComparisonItemResponse(
        Integer year,
        Integer month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netAmount
) {
}
