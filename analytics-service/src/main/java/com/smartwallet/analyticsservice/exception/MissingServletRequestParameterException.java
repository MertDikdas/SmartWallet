package com.smartwallet.analyticsservice.exception;

public class MissingServletRequestParameterException extends RuntimeException {
    public MissingServletRequestParameterException() {
        super("Missing request parameter");
    }
}
