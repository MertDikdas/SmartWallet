package com.smartwallet.financeservice.exception;

public class InvalidRecurringTransactionStateException
        extends RuntimeException {

    public InvalidRecurringTransactionStateException(
            String message
    ) {
        super(message);
    }
}