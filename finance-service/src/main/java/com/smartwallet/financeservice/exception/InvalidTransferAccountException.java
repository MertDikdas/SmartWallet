package com.smartwallet.financeservice.exception;

public class InvalidTransferAccountException extends RuntimeException {
    public InvalidTransferAccountException() {
        super(
                "Source and destination accounts must be different"
        );
    }
}
