package com.smartwallet.analyticsservice.dto.response;

import com.smartwallet.analyticsservice.entity.CurrencyCode;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyCategoryAnalyticsResponse(
        Integer year,
        Integer month,
        BigDecimal totalExpense,
        CurrencyCode currency,
        List<CategoryExpenseResponse> categories
) {
}
