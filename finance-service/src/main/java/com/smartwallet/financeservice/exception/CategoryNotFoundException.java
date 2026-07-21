package com.smartwallet.financeservice.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long categoryId) {
        super("Category could not be found with id: " + categoryId);
    }
}