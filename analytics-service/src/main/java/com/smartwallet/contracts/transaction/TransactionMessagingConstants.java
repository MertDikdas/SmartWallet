package com.smartwallet.contracts.transaction;

public final class TransactionMessagingConstants {

    public static final String EXCHANGE =
            "smartwallet.transaction.exchange";

    public static final String BUDGET_QUEUE =
            "budget.transaction.queue";

    public static final String CREATED_ROUTING_KEY =
            "transaction.created";

    public static final String UPDATED_ROUTING_KEY =
            "transaction.updated";

    public static final String DELETED_ROUTING_KEY =
            "transaction.deleted";

    private TransactionMessagingConstants() {
    }
}