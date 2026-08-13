package com.smartwallet.contracts.recurring;

public final class RecurringTransactionMessagingConstants {

    public static final String EXCHANGE =
            "recurring.transaction.exchange";

    public static final String FAILED_ROUTING_KEY=
            "recurring.transaction.failed";

    public static final String NOTIFICATION_QUEUE=
            "recurring.notification.queue";

    private RecurringTransactionMessagingConstants() {
    }

}
