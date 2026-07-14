package com.smartwallet.authservice.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "security.refresh-token")
public record RefreshTokenProperties(
        @NotNull Duration ttl
) {
}