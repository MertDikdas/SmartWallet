package com.smartwallet.financeservice.dto.response;

import com.smartwallet.financeservice.entity.CurrencyCode;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(

        Long id,

        Long fromAccountId,

        String fromAccountName,

        Long toAccountId,

        String toAccountName,

        BigDecimal amount,

        CurrencyCode currency,

        String description,

        Instant transferredAt,

        Instant createdAt

) {
}