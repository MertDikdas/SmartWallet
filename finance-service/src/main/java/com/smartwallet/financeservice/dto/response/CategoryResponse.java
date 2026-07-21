package com.smartwallet.financeservice.dto.response;

import com.smartwallet.financeservice.entity.TransactionType;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String name,
        TransactionType type,
        Instant createdAt
) {
}