package com.smartwallet.analyticsservice.dto.response;

import com.smartwallet.analyticsservice.entity.CurrencyCode;

import java.math.BigDecimal;

public record MonthlyAnalyticsResponse(
        Integer year,
        Integer month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netAmount,
        Long transactionCount,
        CurrencyCode currency
) {
}