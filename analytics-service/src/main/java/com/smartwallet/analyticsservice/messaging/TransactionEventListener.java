package com.smartwallet.analyticsservice.messaging;

import com.smartwallet.contracts.transaction.TransactionChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    private final TransactionEventHandler eventHandler;

    @RabbitListener(
            queues = AnalyticsMessagingConstants.TRANSACTION_QUEUE
    )
    public void listen(TransactionChangedEvent event) {

        log.info(
                "Transaction event received for analytics. "
                        + "eventId={}, type={}, transactionId={}",
                event.eventId(),
                event.eventType(),
                getTransactionId(event)
        );

        eventHandler.handle(event);
    }

    private Long getTransactionId(
            TransactionChangedEvent event
    ) {
        if (event.after() != null) {
            return event.after().transactionId();
        }

        return event.before().transactionId();
    }
}