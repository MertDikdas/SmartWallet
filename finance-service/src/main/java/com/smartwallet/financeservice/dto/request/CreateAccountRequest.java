package com.smartwallet.financeservice.dto.request;

import com.smartwallet.financeservice.entity.AccountType;
import com.smartwallet.financeservice.entity.CurrencyCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "Account name is required")
        @Size(
                max = 100,
                message = "Account name cannot exceed 100 characters"
        )
        String name,

        @NotNull(message = "Account type is required")
        AccountType type,

        @NotNull(message = "Currency is required")
        CurrencyCode currency,

        @NotNull(message = "Initial balance is required")
        @DecimalMin(
                value = "0.00",
                message = "Initial balance cannot be negative"
        )
        BigDecimal initialBalance

) {
}