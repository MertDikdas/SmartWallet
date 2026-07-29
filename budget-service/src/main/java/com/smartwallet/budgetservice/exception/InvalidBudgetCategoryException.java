package com.smartwallet.budgetservice.exception;

public class InvalidBudgetCategoryException extends RuntimeException {
    public InvalidBudgetCategoryException(Long categoryId) {
        super(
                "Category is unavailable or does not belong " +
                        "to the current user. Category id: " +
                        categoryId
        );
    }
}
