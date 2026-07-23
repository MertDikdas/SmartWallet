package com.smartwallet.contracts.budget;

public final class BudgetMessagingConstants {
    public static final String EXCHANGE =
            "smartwallet.budget.exchange";

    public static final String NOTIFICATION_QUEUE =
            "notification.budget.queue";

    public static final String BUDGET_EXCEEDED_ROUTING_KEY =
            "budget.exceeded";
    private BudgetMessagingConstants() {
    }
}
