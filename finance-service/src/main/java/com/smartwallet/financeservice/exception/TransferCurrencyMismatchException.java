package com.smartwallet.financeservice.exception;

public class TransferCurrencyMismatchException extends RuntimeException {
    public TransferCurrencyMismatchException() {
        super("Source and destination accounts must have the same currency for transfer.");
    }
}
