package com.smartwallet.financeservice.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super(
                "The idempotency key was already used with different transfer data"
        );
    }
}
