package com.smartwallet.analyticsservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DailyCashFlowResponse(
        Integer year,
        Integer month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        List<DailyCashFlowItemResponse> days
) {
}