package com.smartwallet.budgetservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwallet.contracts.budget.BudgetMessagingConstants;
import com.smartwallet.contracts.transaction.TransactionMessagingConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange transactionExchange() {
        return new TopicExchange(
                TransactionMessagingConstants.EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public TopicExchange budgetExchange() {
        return new TopicExchange(
                BudgetMessagingConstants.EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue budgetTransactionQueue() {
        return QueueBuilder
                .durable(
                        TransactionMessagingConstants.BUDGET_QUEUE
                )
                .build();
    }

    @Bean
    public Binding transactionCreatedBinding(
            Queue budgetTransactionQueue,
            @Qualifier("transactionExchange")
            TopicExchange transactionExchange
    ) {
        return BindingBuilder
                .bind(budgetTransactionQueue)
                .to(transactionExchange)
                .with(
                        TransactionMessagingConstants
                                .CREATED_ROUTING_KEY
                );
    }

    @Bean
    public Binding transactionUpdatedBinding(
            Queue budgetTransactionQueue,
            @Qualifier("transactionExchange")
            TopicExchange transactionExchange
    ) {
        return BindingBuilder
                .bind(budgetTransactionQueue)
                .to(transactionExchange)
                .with(
                        TransactionMessagingConstants
                                .UPDATED_ROUTING_KEY
                );
    }

    @Bean
    public Binding transactionDeletedBinding(
            Queue budgetTransactionQueue,
            @Qualifier("transactionExchange")
            TopicExchange transactionExchange
    ) {
        return BindingBuilder
                .bind(budgetTransactionQueue)
                .to(transactionExchange)
                .with(
                        TransactionMessagingConstants
                                .DELETED_ROUTING_KEY
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
                "com.smartwallet.contracts.transaction",
                "com.smartwallet.contracts.budget"
        );

        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }
}