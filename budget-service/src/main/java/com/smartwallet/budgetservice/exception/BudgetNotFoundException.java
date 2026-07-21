package com.smartwallet.budgetservice.exception;

public class BudgetNotFoundException extends RuntimeException {

    public BudgetNotFoundException(Long budgetId) {
        super("Budget could not be found with id: " + budgetId);
    }
}