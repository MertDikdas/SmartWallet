package com.smartwallet.financeservice.exception;

public class RecurringTransactionCategoryMismatchException
        extends RuntimeException {

    public RecurringTransactionCategoryMismatchException() {
        super(
                "Category type must match the recurring transaction type"
        );
    }
}