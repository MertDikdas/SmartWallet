package com.smartwallet.notificationservice.messaging;

import com.smartwallet.contracts.budget.BudgetExceededEvent;
import com.smartwallet.contracts.budget.BudgetMessagingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetExceededEventListener {

    private final BudgetExceededEventHandler eventHandler;

    @RabbitListener(
            queues = BudgetMessagingConstants.NOTIFICATION_QUEUE
    )
    public void listen(BudgetExceededEvent event) {

        log.info(
                "Budget exceeded event received. eventId={}, budgetId={}",
                event.eventId(),
                event.budgetId()
        );

        eventHandler.handle(event);
    }
}