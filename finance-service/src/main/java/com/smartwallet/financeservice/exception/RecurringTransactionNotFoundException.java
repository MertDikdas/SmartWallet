package com.smartwallet.financeservice.exception;

public class RecurringTransactionNotFoundException  extends RuntimeException{
    public RecurringTransactionNotFoundException(){
        super("Recurring transaction was not found");
    }
}
