package com.smartwallet.financeservice.exception;

public class FinancialTransactionNotFoundException
        extends RuntimeException {

    public FinancialTransactionNotFoundException(Long transactionId) {
        super(
                "Transaction could not be found with id: "
                        + transactionId
        );
    }
}