package com.smartwallet.notificationservice.messaging;

import com.smartwallet.contracts.recurring.RecurringTransactionFailedEvent;
import com.smartwallet.notificationservice.repository.NotificationRepository;
import com.smartwallet.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecurringTransactionFailedEventHandler {

    private final NotificationRepository notificationRepository;
    private NotificationService notificationService;

    @Transactional
    public void handle(RecurringTransactionFailedEvent event) {
        String title = "Recurring transaction paused";

        String message =
                "Your recurring transaction was paused after "
                        + event.attemptCount()
                        + " failed attempts. "
                        + "Scheduled date: "
                        + event.scheduledDate()
                        + ". Error: "
                        + event.errorMessage();
        notificationRepository.insertRecurringTransactionFailedNotification(
                event.userId(),
                title,
                message,
                event.recurringTransactionId(),
                event.eventId()
        );
    }
}
