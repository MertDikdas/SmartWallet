package com.smartwallet.financeservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwallet.contracts.recurring.RecurringTransactionMessagingConstants;
import com.smartwallet.contracts.transaction.TransactionMessagingConstants;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;

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
    public TopicExchange recurringTransactionExchange(){
        return new TopicExchange(
                RecurringTransactionMessagingConstants.EXCHANGE,
                true,
                false
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
                 "com.smartwallet.contracts.recurring"
        );

        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }
}