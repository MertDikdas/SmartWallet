package com.smartwallet.budgetservice.dto.request;

public record CategoryBudgetRequest(
        Long categoryId,
        Integer year,
        Integer month
) {
}
