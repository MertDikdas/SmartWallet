package com.smartwallet.financeservice.dto.request;

import com.smartwallet.financeservice.entity.AccountType;
import com.smartwallet.financeservice.entity.CurrencyCode;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(

        @Size(
                min = 1,
                max = 100,
                message = "Account name must be between 1 and 100 characters"
        )
        String name,

        AccountType type,

        CurrencyCode currency

) {
}