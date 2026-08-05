package com.smartwallet.notificationservice.messaging;

import com.smartwallet.contracts.recurring.RecurringTransactionFailedEvent;
import com.smartwallet.contracts.recurring.RecurringTransactionMessagingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionFailedEventListener {
    private final RecurringTransactionFailedEventHandler
            eventHandler;

    @RabbitListener(
            queues = RecurringTransactionMessagingConstants.NOTIFICATION_QUEUE
    )
    public void listen(
            RecurringTransactionFailedEvent event
    ){
        log.info(
                "Recurring transaction failed event received. eventId={}" +
                        "recurringTransactionId = {}, userId = {}",
                event.eventId(),
                event.recurringTransactionId(),
                event.userId()
        );

        eventHandler.handle(event);

    }
}
