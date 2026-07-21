package com.smartwallet.financeservice.exception;

public class CategoryTypeMismatchException
        extends RuntimeException {

    public CategoryTypeMismatchException() {
        super(
                "Category type must match the transaction type"
        );
    }
}