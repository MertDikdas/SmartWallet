package com.smartwallet.budgetservice.messaging;

import com.smartwallet.contracts.transaction.TransactionChangedEvent;
import com.smartwallet.contracts.transaction.TransactionMessagingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionChangedEventListener {

    private final BudgetTransactionEventHandler eventHandler;

    @RabbitListener(
            queues = TransactionMessagingConstants.BUDGET_QUEUE
    )
    public void listen(TransactionChangedEvent event) {

        log.info(
                "Transaction event received. eventId={}, type={}",
                event.eventId(),
                event.eventType()
        );

        eventHandler.handle(event);
    }
}