package com.smartwallet.budgetservice.exception;

public class BudgetAlreadyExistsException extends RuntimeException {

    public BudgetAlreadyExistsException(
            Long categoryId,
            Integer year,
            Integer month
    ) {
        super(
                "Budget already exists for category "
                        + categoryId
                        + " in "
                        + month
                        + "/"
                        + year
        );
    }
}