package com.smartwallet.financeservice.config;

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
    public RestClient budgetRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${BUDGET_SERVICE_URL:http://localhost:8083}")
            String budgetBaseUrl
    ) {
        return restClientBuilder
                .baseUrl(budgetBaseUrl)
                .build();
    }

}
