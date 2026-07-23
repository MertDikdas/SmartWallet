package com.smartwallet.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwallet.contracts.budget.BudgetMessagingConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
            Queue notificationBudgetQueue,
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
    public MessageConverter rabbitMessageConverter(
            ObjectMapper objectMapper
    ) {
        Jackson2JsonMessageConverter converter =
                new Jackson2JsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper typeMapper =
                new DefaultJackson2JavaTypeMapper();

        typeMapper.setTrustedPackages(
                "com.smartwallet.contracts.budget"
        );

        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

}
