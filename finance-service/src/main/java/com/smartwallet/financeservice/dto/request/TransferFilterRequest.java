package com.smartwallet.financeservice.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record TransferFilterRequest(

        @Positive(message = "Account ID must be positive")
        Long accountId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant endDate,

        @Min(
                value = 0,
                message = "Page cannot be negative"
        )
        Integer page,

        @Min(
                value = 1,
                message = "Page size must be at least 1"
        )
        @Max(
                value = 100,
                message = "Page size cannot exceed 100"
        )
        Integer size

) {

    public int resolvedPage() {
        return page != null
                ? page
                : 0;
    }

    public int resolvedSize() {
        return size != null
                ? size
                : 20;
    }

    @AssertTrue(
            message = "Start date must be before or equal to end date"
    )
    public boolean isDateRangeValid() {
        return startDate == null
                || endDate == null
                || !startDate.isAfter(endDate);
    }
}