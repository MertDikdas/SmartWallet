package com.smartwallet.financeservice.mapper;

import com.smartwallet.financeservice.dto.response.TransactionResponse;
import com.smartwallet.financeservice.entity.FinancialTransaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(
            FinancialTransaction transaction
    ) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt()
        );
    }
}