package com.smartwallet.analyticsservice.dto.projection;

import java.math.BigDecimal;

public record DailyCashFlowAggregate(
        Integer day,
        BigDecimal totalIncome,
        BigDecimal totalExpense
) {
}