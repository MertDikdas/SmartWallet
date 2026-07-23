package com.smartwallet.notificationservice.exception;

public class NotificationNotFoundException
        extends RuntimeException {

    public NotificationNotFoundException(
            Long notificationId
    ) {
        super(
                "Notification could not be found with id: "
                        + notificationId
        );
    }
}