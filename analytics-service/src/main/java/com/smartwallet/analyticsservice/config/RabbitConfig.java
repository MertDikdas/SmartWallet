package com.smartwallet.analyticsservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwallet.analyticsservice.messaging.AnalyticsMessagingConstants;
import com.smartwallet.contracts.transaction.TransactionMessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
    public Queue analyticsTransactionQueue() {
        return QueueBuilder
                .durable(
                        AnalyticsMessagingConstants.TRANSACTION_QUEUE
                )
                .build();
    }

    @Bean
    public Binding transactionCreatedBinding(
            Queue analyticsTransactionQueue,
            TopicExchange transactionExchange
    ) {
        return BindingBuilder
                .bind(analyticsTransactionQueue)
                .to(transactionExchange)
                .with(
                        TransactionMessagingConstants.CREATED_ROUTING_KEY
                );
    }

    @Bean
    public Binding transactionUpdatedBinding(
            Queue analyticsTransactionQueue,
            TopicExchange transactionExchange
    ) {
        return BindingBuilder
                .bind(analyticsTransactionQueue)
                .to(transactionExchange)
                .with(
                        TransactionMessagingConstants.UPDATED_ROUTING_KEY
                );
    }

    @Bean
    public Binding transactionDeletedBinding(
            Queue analyticsTransactionQueue,
            TopicExchange transactionExchange
    ) {
        return BindingBuilder
                .bind(analyticsTransactionQueue)
                .to(transactionExchange)
                .with(
                        TransactionMessagingConstants.DELETED_ROUTING_KEY
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
                "com.smartwallet.contracts.transaction"
        );

        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }
}