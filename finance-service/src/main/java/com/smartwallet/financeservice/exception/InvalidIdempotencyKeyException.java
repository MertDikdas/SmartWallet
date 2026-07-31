package com.smartwallet.financeservice.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException() {
        super(
                "Idempotency-Key must not be blank and cannot exceed 100 characters"
        );
    }
}
