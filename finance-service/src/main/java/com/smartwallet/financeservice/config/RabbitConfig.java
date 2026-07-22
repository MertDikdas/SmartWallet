package com.smartwallet.financeservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwallet.contracts.transaction.TransactionMessagingConstants;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
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
    public MessageConverter rabbitMessageConverter(
            ObjectMapper objectMapper
    ) {
        Jackson2JsonMessageConverter converter =
                new Jackson2JsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper typeMapper =
                new DefaultJackson2JavaTypeMapper();

        typeMapper.setTrustedPackages(
                "com.smartwallet.contracts.transaction"
        );

        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }
}