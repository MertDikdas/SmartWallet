package com.smartwallet.notificationservice.messaging;

import com.smartwallet.contracts.recurring.RecurringTransactionFailedEvent;
import com.smartwallet.notificationservice.repository.NotificationRepository;
import com.smartwallet.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class RecurringTransactionFailedEventHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private RecurringTransactionFailedEventHandler eventHandler;


    @Test
    void shouldCreateNotificationForRecurringTransactionFailure(){

        UUID eventId =
                UUID.fromString(
                        "b3d2610e-8f85-4a01-9a12-29864e602e72"
                );

        RecurringTransactionFailedEvent event = new RecurringTransactionFailedEvent(
                eventId,
                Instant.parse("2026-08-04T12:00:00Z"),
                25L,
                10L,
                LocalDate.of(2026, 8, 4),
                3,
                "Account was not found"
        );
        eventHandler.handle(event);

        verify(notificationRepository)
                .insertRecurringTransactionFailedNotification(
                        10L,
                        "Recurring transaction paused",
                        "Your recurring transaction was paused after "
                                + "3 failed attempts. "
                                + "Scheduled date: 2026-08-04. "
                                + "Error: Account was not found",
                        25L,
                        eventId
                );

        verifyNoMoreInteractions(
                notificationRepository
        );

    }
}