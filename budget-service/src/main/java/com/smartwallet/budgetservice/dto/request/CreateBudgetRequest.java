package com.smartwallet.budgetservice.dto.request;

import com.smartwallet.budgetservice.entity.CurrencyCode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateBudgetRequest(

        @NotNull(message = "Category ID is required")
        @Positive(message = "Category ID must be positive")
        Long categoryId,

        @NotNull(message = "Budget limit is required")
        @DecimalMin(
                value = "0.01",
                message = "Budget limit must be greater than zero"
        )
        BigDecimal limitAmount,

        @NotNull(message = "Year is required")
        @Min(value = 2000, message = "Year must be at least 2000")
        Integer year,

        @NotNull(message = "Month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        Integer month,
        @NotNull
        CurrencyCode currency

) {
}