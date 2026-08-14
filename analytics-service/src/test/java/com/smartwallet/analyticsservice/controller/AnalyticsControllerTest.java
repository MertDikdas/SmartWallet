package com.smartwallet.analyticsservice.controller;

import com.smartwallet.analyticsservice.config.SecurityConfig;
import com.smartwallet.analyticsservice.dto.response.MonthlyAnalyticsResponse;
import com.smartwallet.analyticsservice.dto.response.MonthlyCategoryAnalyticsResponse;
import com.smartwallet.analyticsservice.dto.response.MonthlyComparisonItemResponse;
import com.smartwallet.analyticsservice.dto.response.MonthlyComparisonResponse;
import com.smartwallet.analyticsservice.dto.response.MonthlyTrendItemResponse;
import com.smartwallet.analyticsservice.dto.response.MonthlyTrendResponse;
import com.smartwallet.analyticsservice.dto.response.YearlyAnalyticsResponse;
import com.smartwallet.analyticsservice.entity.CurrencyCode;
import com.smartwallet.analyticsservice.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnMonthlyAnalyticsForAuthenticatedUser()
            throws Exception {

        MonthlyAnalyticsResponse response =
                new MonthlyAnalyticsResponse(
                        2026,
                        7,
                        new BigDecimal("5000.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("3000.00"),
                        4L,
                        CurrencyCode.TRY
                );

        when(
                analyticsService.getMonthlyAnalytics(
                        1L,
                        2026,
                        7,
                        CurrencyCode.TRY
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/analytics/monthly")
                                .param("year", "2026")
                                .param("month", "7")
                                .param("currency", "TRY")
                                .with(
                                        jwt().jwt(
                                                token -> token.subject("1")
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.year")
                                .value(2026)
                )
                .andExpect(
                        jsonPath("$.month")
                                .value(7)
                )
                .andExpect(
                        jsonPath("$.totalIncome")
                                .value(5000.00)
                )
                .andExpect(
                        jsonPath("$.totalExpense")
                                .value(2000.00)
                )
                .andExpect(
                        jsonPath("$.netAmount")
                                .value(3000.00)
                )
                .andExpect(
                        jsonPath("$.transactionCount")
                                .value(4)
                );

        verify(analyticsService)
                .getMonthlyAnalytics(
                        1L,
                        2026,
                        7,
                        CurrencyCode.TRY
                );
    }

    @Test
    void shouldReturnUnauthorizedWithoutJwt()
            throws Exception {

        mockMvc.perform(
                        get("/api/analytics/monthly")
                                .param("year", "2026")
                                .param("month", "7")
                                .param("currency", "TRY")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(analyticsService);
    }

    @Test
    void shouldReturnBadRequestForInvalidMonth()
            throws Exception {

        mockMvc.perform(
                        get("/api/analytics/monthly")
                                .param("year", "2026")
                                .param("month", "13")
                                .param("currency", "TRY")
                                .with(
                                        jwt().jwt(
                                                token -> token.subject("1")
                                        )
                                )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(analyticsService);
    }

    @Test
    void shouldReturnBadRequestForInvalidYear()
            throws Exception {

        mockMvc.perform(
                        get("/api/analytics/monthly")
                                .param("year", "1999")
                                .param("month", "7")
                                .param("currency", "TRY")
                                .with(
                                        jwt().jwt(
                                                token -> token.subject("1")
                                        )
                                )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(analyticsService);
    }

    @Test
    void shouldUseSixMonthsAsDefaultTrendPeriod()
            throws Exception {

        MonthlyTrendResponse response =
                new MonthlyTrendResponse(
                        List.of(
                                new MonthlyTrendItemResponse(
                                        2026,
                                        7,
                                        new BigDecimal("5000.00"),
                                        new BigDecimal("2000.00"),
                                        new BigDecimal("3000.00"),
                                        4L
                                )
                        ),
                        CurrencyCode.TRY
                );

        when(
                analyticsService.getMonthlyTrend(
                        1L,
                        6,
                        CurrencyCode.TRY
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/analytics/monthly-trend")
                                .param("currency", "TRY")
                                .with(
                                        jwt().jwt(
                                                token -> token.subject("1")
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.months")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.months.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.months[0].year")
                                .value(2026)
                )
                .andExpect(
                        jsonPath("$.months[0].month")
                                .value(7)
                );

        verify(analyticsService)
                .getMonthlyTrend(
                        1L,
                        6,
                        CurrencyCode.TRY
                );
    }

    @Test
    void shouldUseRequestedTrendPeriod()
            throws Exception {

        MonthlyTrendResponse response =
                new MonthlyTrendResponse(List.of(),CurrencyCode.TRY);

        when(
                analyticsService.getMonthlyTrend(
                        5L,
                        3,
                        CurrencyCode.TRY
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/analytics/monthly-trend")
                                .param("months", "3")
                                .param("currency", "TRY")
                                .with(
                                        jwt().jwt(
                                                token -> token.subject("5")
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.months")
                                .isEmpty()
                );

        verify(analyticsService)
                .getMonthlyTrend(
                        5L,
                        3,
                        CurrencyCode.TRY
                );
    }

    @Test
    void shouldCompareTwoRequestedPeriods()
            throws Exception {

        MonthlyComparisonItemResponse basePeriod =
                new MonthlyComparisonItemResponse(
                        2026,
                        5,
                        new BigDecimal("4000.00"),
                        new BigDecimal("2500.00"),
                        new BigDecimal("1500.00")
                );

        MonthlyComparisonItemResponse comparisonPeriod =
                new MonthlyComparisonItemResponse(
                        2026,
                        7,
                        new BigDecimal("5000.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("3000.00")
                );

        MonthlyComparisonResponse response =
                new MonthlyComparisonResponse(
                        basePeriod,
                        comparisonPeriod,
                        new BigDecimal("25.00"),
                        new BigDecimal("-20.00"),
                        CurrencyCode.TRY
                );

        when(
                analyticsService.getMonthlyComparison(
                        1L,
                        2026,
                        5,
                        2026,
                        7,
                        CurrencyCode.TRY
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/analytics/monthly-comparison")
                                .param("baseYear", "2026")
                                .param("baseMonth", "5")
                                .param("comparisonYear", "2026")
                                .param("comparisonMonth", "7")
                                .param("currency", "TRY")
                                .with(
                                        jwt().jwt(
                                                token -> token.subject("1")
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.basePeriod.month")
                                .value(5)
                )
                .andExpect(
                        jsonPath("$.comparisonPeriod.month")
                                .value(7)
                )
                .andExpect(
                        jsonPath("$.incomeChangePercentage")
                                .value(25.00)
                )
                .andExpect(
                        jsonPath("$.expenseChangePercentage")
                                .value(-20.00)
                );

        verify(analyticsService)
                .getMonthlyComparison(
                        1L,
                        2026,
                        5,
                        2026,
                        7,
                        CurrencyCode.TRY
                );
    }

    @Test
    void shouldReturnYearlyAnalytics()
            throws Exception {

        YearlyAnalyticsResponse response =
                new YearlyAnalyticsResponse(
                        2026,
                        new BigDecimal("60000.00"),
                        new BigDecimal("36000.00"),
                        new BigDecimal("24000.00"),
                        120L,
                        new BigDecimal("3000.00"),
                        CurrencyCode.TRY
                );

        when(
                analyticsService.getYearlyAnalytics(
                        1L,
                        2026,
                        CurrencyCode.TRY
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/analytics/yearly")
                                .param("year", "2026")
                                .param("currency", "TRY")
                                .with(
                                        jwt().jwt(
                                                token -> token.subject("1")
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.year")
                                .value(2026)
                )
                .andExpect(
                        jsonPath("$.totalIncome")
                                .value(60000.00)
                )
                .andExpect(
                        jsonPath("$.totalExpense")
                                .value(36000.00)
                )
                .andExpect(
                        jsonPath("$.netAmount")
                                .value(24000.00)
                )
                .andExpect(
                        jsonPath("$.averageMonthlyExpense")
                                .value(3000.00)
                );

        verify(analyticsService)
                .getYearlyAnalytics(
                        1L,
                        2026,
                        CurrencyCode.TRY
                );
    }

    @Test
    void shouldReturnMonthlyCategoryAnalytics()
            throws Exception {

        MonthlyCategoryAnalyticsResponse response =
                new MonthlyCategoryAnalyticsResponse(
                        2026,
                        7,
                        new BigDecimal("1500.00"),
                        CurrencyCode.TRY,
                        List.of()
                );

        when(
                analyticsService.getMonthlyCategoryAnalytics(
                        1L,
                        2026,
                        7,
                        CurrencyCode.TRY
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/analytics/monthly/categories")
                                .param("year", "2026")
                                .param("month", "7")
                                .param("currency", "TRY")
                                .with(
                                        jwt().jwt(
                                                token -> token.subject("1")
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.year")
                                .value(2026)
                )
                .andExpect(
                        jsonPath("$.month")
                                .value(7)
                )
                .andExpect(
                        jsonPath("$.totalExpense")
                                .value(1500.00)
                )
                .andExpect(
                        jsonPath("$.categories")
                                .isArray()
                );

        verify(analyticsService)
                .getMonthlyCategoryAnalytics(
                        1L,
                        2026,
                        7,
                        CurrencyCode.TRY
                );
    }
}