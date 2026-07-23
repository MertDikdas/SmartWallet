package com.smartwallet.analyticsservice.dto.projection;

import java.math.BigDecimal;

public record MonthlyAggregate(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        Long transactionCount
) {
}