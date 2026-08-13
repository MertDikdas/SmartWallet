package com.smartwallet.budgetservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient financeRestClient(
            RestClient.Builder builder,
            @Value("${services.finance.base-url}")
            String financeBaseUrl
    ) {
        return builder
                .baseUrl(financeBaseUrl)
                .build();
    }
}