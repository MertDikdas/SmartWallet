package com.smartwallet.financeservice.exception;

public class CategoryCannotDeleteBecauseTransactions extends RuntimeException{
    public CategoryCannotDeleteBecauseTransactions(){
        super(
                "Some transactions exists with this category!"
        );
    }
}
