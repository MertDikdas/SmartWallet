package com.smartwallet.budgetservice.client.dto;

import java.time.Instant;

public record CategoryDetailsResponse(
        Long id,
        String name,
        String type,
        Instant createdAt
) {
}