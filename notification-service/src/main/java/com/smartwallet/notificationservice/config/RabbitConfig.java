package com.smartwallet.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwallet.contracts.budget.BudgetMessagingConstants;
import com.smartwallet.contracts.recurring.RecurringTransactionMessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange budgetExchange() {
        return new TopicExchange(
                BudgetMessagingConstants.EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue notificationBudgetQueue() {
        return QueueBuilder
                .durable(
                        BudgetMessagingConstants
                                .NOTIFICATION_QUEUE
                )
                .build();
    }

    @Bean
    public Binding budgetExceededBinding(
            @Qualifier("notificationBudgetQueue")
            Queue notificationBudgetQueue,

            @Qualifier("budgetExchange")
            TopicExchange budgetExchange
    ) {
        return BindingBuilder
                .bind(notificationBudgetQueue)
                .to(budgetExchange)
                .with(
                        BudgetMessagingConstants
                                .BUDGET_EXCEEDED_ROUTING_KEY
                );
    }

    @Bean
    public TopicExchange recurringTransactionExchange() {
        return new TopicExchange(
                RecurringTransactionMessagingConstants.EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue recurringTransactionFailureNotificationQueue() {
        return QueueBuilder
                .durable(
                        RecurringTransactionMessagingConstants
                                .NOTIFICATION_QUEUE
                )
                .build();
    }

    @Bean
    public Binding recurringTransactionFailureBinding(
            @Qualifier(
                    "recurringTransactionFailureNotificationQueue"
            )
            Queue recurringTransactionFailureNotificationQueue,

            @Qualifier("recurringTransactionExchange")
            TopicExchange recurringTransactionExchange
    ) {
        return BindingBuilder
                .bind(
                        recurringTransactionFailureNotificationQueue
                )
                .to(recurringTransactionExchange)
                .with(
                        RecurringTransactionMessagingConstants
                                .FAILED_ROUTING_KEY
                );
    }

    @Bean
    public MessageConverter rabbitMessageConverter(
            ObjectMapper objectMapper
    ) {
        JacksonJsonMessageConverter converter =
                new JacksonJsonMessageConverter();

        DefaultJacksonJavaTypeMapper typeMapper =
                new DefaultJacksonJavaTypeMapper();

        typeMapper.setTrustedPackages(
                "com.smartwallet.contracts.budget",
                "com.smartwallet.contracts.recurring"
        );

        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }
}