package com.smartwallet.budgetservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateBudgetRequest(

        @NotNull(message = "Budget limit is required")
        @DecimalMin(
                value = "0.01",
                message = "Budget limit must be greater than zero"
        )
        BigDecimal limitAmount

) {
}