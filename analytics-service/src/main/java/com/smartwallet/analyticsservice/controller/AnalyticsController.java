package com.smartwallet.analyticsservice.controller;

import com.smartwallet.analyticsservice.dto.response.MonthlyAnalyticsResponse;
import com.smartwallet.analyticsservice.service.AnalyticsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyAnalyticsResponse>
    getMonthlyAnalytics(
            @AuthenticationPrincipal Jwt jwt,

            @RequestParam
            @Min(value = 2000)
            int year,

            @RequestParam
            @Min(value = 1)
            @Max(value = 12)
            int month
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                analyticsService.getMonthlyAnalytics(
                        userId,
                        year,
                        month
                )
        );
    }
}