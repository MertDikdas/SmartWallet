package com.smartwallet.financeservice.mapper;


import com.smartwallet.financeservice.dto.response.TransferResponse;
import com.smartwallet.financeservice.entity.AccountTransfer;
import org.springframework.stereotype.Component;

@Component
public class AccountTransferMapper {

    public TransferResponse toResponse(
            AccountTransfer accountTransfer
    ) {
        return new  TransferResponse(
                accountTransfer.getId(),
                accountTransfer.getFromAccount().getId(),
                accountTransfer.getFromAccount().getName(),
                accountTransfer.getToAccount().getId(),
                accountTransfer.getToAccount().getName(),
                accountTransfer.getAmount(),
                accountTransfer.getCurrency(),
                accountTransfer.getDescription(),
                accountTransfer.getTransferredAt(),
                accountTransfer.getCreatedAt()
        );
    }
}
