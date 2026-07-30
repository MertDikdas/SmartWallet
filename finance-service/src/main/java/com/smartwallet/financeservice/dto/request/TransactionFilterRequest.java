package com.smartwallet.financeservice.dto.request;


import com.smartwallet.financeservice.entity.TransactionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.AssertTrue;

import java.time.Instant;

public record TransactionFilterRequest(
        @Positive(message = "Account ID must be a positive number")
        Long accountId,
        @Positive(message = "Category ID must be a positive number")
        Long categoryId,
        TransactionType type,
        @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE_TIME
        )
        Instant startDate,
        @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE_TIME
        )
        Instant endDate,
        @Min(
                value = 0,
                message = "Page number must be greater than or equal to 0"
        )
        Integer page,
        @Min(
                value = 1,
                message = "Page size must be greater than or equal to 1"
        )
        @Max(
                value = 100,
                message = "Page size must be less than or equal to 100"
        )
        Integer size

) {
    public int resolvedPage(){
        return page != null
                ? page
                : 0;
    }

    public int resolvedSize(){
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
