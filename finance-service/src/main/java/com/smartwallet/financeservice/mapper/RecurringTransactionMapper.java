package com.smartwallet.financeservice.mapper;

import com.smartwallet.financeservice.dto.response.RecurringTransactionResponse;
import com.smartwallet.financeservice.entity.RecurringTransaction;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionMapper {

    public RecurringTransactionResponse toResponse(RecurringTransaction transaction){
        return new RecurringTransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getFrequency(),
                transaction.getStatus(),
                transaction.getStartDate(),
                transaction.getEndDate(),
                transaction.getNextExecutionDate(),
                transaction.getLastExecutionDate(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
