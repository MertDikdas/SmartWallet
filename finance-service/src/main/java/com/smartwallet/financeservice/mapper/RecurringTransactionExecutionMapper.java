package com.smartwallet.financeservice.mapper;

import com.smartwallet.financeservice.dto.response.RecurringTransactionExecutionResponse;
import com.smartwallet.financeservice.entity.RecurringTransactionExecution;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionExecutionMapper {

    public RecurringTransactionExecutionResponse toResponse(
            RecurringTransactionExecution execution
    ) {
        return new RecurringTransactionExecutionResponse(
                execution.getId(),
                execution.getScheduledDate(),
                execution.getStatus(),
                execution.getGeneratedTransactionId(),
                execution.getErrorMessage(),
                execution.getAttemptCount(),
                execution.getNextRetryAt(),
                execution.getCreatedAt(),
                execution.getCompletedAt()
        );
    }
}