package com.smartwallet.financeservice.messaging;

import com.smartwallet.contracts.transaction.TransactionChangedEvent;
import com.smartwallet.contracts.transaction.TransactionMessagingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitTransactionEventRelay {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void publish(TransactionChangedEvent event) {
        rabbitTemplate.convertAndSend(
                TransactionMessagingConstants.EXCHANGE,
                event.routingKey(),
                event
        );

        log.info(
                "Transaction event published. eventId={}, type={}, routingKey={}",
                event.eventId(),
                event.eventType(),
                event.routingKey()
        );
    }
}