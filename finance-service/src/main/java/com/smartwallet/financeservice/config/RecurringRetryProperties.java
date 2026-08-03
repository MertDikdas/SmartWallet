package com.smartwallet.financeservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(
        prefix = "app.recurring-transactions.retry"
)
public class RecurringRetryProperties {

    private int maxAttempts = 3;

    private Duration firstDelay =
            Duration.ofMinutes(1);

    private Duration secondDelay =
            Duration.ofMinutes(5);
}