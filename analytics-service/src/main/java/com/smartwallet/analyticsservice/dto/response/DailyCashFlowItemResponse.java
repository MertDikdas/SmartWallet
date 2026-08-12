package com.smartwallet.analyticsservice.dto.response;

import java.math.BigDecimal;

public record DailyCashFlowItemResponse(
        Integer day,
        BigDecimal totalIncome,
        BigDecimal totalExpense
) {
}