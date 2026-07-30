package com.smartwallet.financeservice.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Source account does not have sufficient balance to complete the transfer.");
    }
}
