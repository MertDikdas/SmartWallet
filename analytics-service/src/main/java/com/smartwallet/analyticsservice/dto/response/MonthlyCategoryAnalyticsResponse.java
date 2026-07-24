package com.smartwallet.analyticsservice.dto.response;

import com.smartwallet.analyticsservice.dto.projection.CategoryExpenseAggregate;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyCategoryAnalyticsResponse(
        Integer year,
        Integer month,
        BigDecimal totalExpense,
        List<CategoryExpenseResponse> categories
) {
}
