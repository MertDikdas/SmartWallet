package com.smartwallet.analyticsservice.dto.response;

import java.math.BigDecimal;

public record MonthlyComparisonResponse(
        MonthlyComparisonItemResponse basePeriod,
        MonthlyComparisonItemResponse comparisonPeriod,
        BigDecimal incomeChangePercentage,
        BigDecimal expenseChangePercentage
) {
}
