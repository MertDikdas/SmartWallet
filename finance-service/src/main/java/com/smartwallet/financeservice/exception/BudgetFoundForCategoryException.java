package com.smartwallet.financeservice.exception;

public class BudgetFoundForCategoryException extends RuntimeException {

    public BudgetFoundForCategoryException() {
        super("A budget found for the category");
    }
}