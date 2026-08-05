package com.smartwallet.notificationservice.messaging;

import com.smartwallet.contracts.budget.BudgetExceededEvent;
import com.smartwallet.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetExceededEventHandler {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void handle(BudgetExceededEvent event) {

        String title =
                "Budget limit exceeded";

        String message =
                "Your budget limit has been exceeded. "
                        + "Limit: "
                        + event.limitAmount()
                        + ", spent: "
                        + event.spentAmount()
                        + ", period: "
                        + event.month()
                        + "/"
                        + event.year();

        notificationRepository
                .insertBudgetExceededNotification(
                        event.userId(),
                        title,
                        message,
                        event.budgetId(),
                        event.eventId()
                );
    }
}