package com.smartwallet.authservice.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User could not be found with userId: " + userId);
    }

}
