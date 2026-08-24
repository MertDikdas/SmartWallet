package com.smartwallet.analyticsservice.service;

import com.smartwallet.analyticsservice.dto.projection.CategoryExpenseAggregate;
import com.smartwallet.analyticsservice.dto.projection.DailyCashFlowAggregate;
import com.smartwallet.analyticsservice.dto.projection.MonthlyAggregate;
import com.smartwallet.analyticsservice.dto.response.DailyCashFlowResponse;
import com.smartwallet.analyticsservice.dto.response.MonthlyAnalyticsResponse;
import com.smartwallet.analyticsservice.dto.response.MonthlyCategoryAnalyticsResponse;
import com.smartwallet.analyticsservice.dto.response.MonthlyComparisonResponse;
import com.smartwallet.analyticsservice.dto.response.MonthlyTrendResponse;
import com.smartwallet.analyticsservice.dto.response.YearlyAnalyticsResponse;
import com.smartwallet.analyticsservice.entity.CurrencyCode;
import com.smartwallet.analyticsservice.entity.ProjectionTransactionType;
import com.smartwallet.analyticsservice.repository.TransactionProjectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TransactionProjectionRepository
            transactionProjectionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void shouldCalculateMonthlyAnalytics() {
        Instant startDate =
                Instant.parse("2026-07-01T00:00:00Z");

        Instant endDate =
                Instant.parse("2026-08-01T00:00:00Z");

        MonthlyAggregate aggregate =
                new MonthlyAggregate(
                        new BigDecimal("5000.00"),
                        new BigDecimal("2000.00"),
                        4L
                );

        when(
                transactionProjectionRepository
                        .calculateMonthlyAggregate(
                                eq(1L),
                                eq(startDate),
                                eq(endDate),
                                eq(ProjectionTransactionType.INCOME),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(aggregate);

        MonthlyAnalyticsResponse response =
                analyticsService.getMonthlyAnalytics(
                        1L,
                        2026,
                        7,
                        CurrencyCode.TRY
                );

        assertThat(response.year())
                .isEqualTo(2026);

        assertThat(response.month())
                .isEqualTo(7);

        assertThat(response.totalIncome())
                .isEqualByComparingTo("5000.00");

        assertThat(response.totalExpense())
                .isEqualByComparingTo("2000.00");

        assertThat(response.netAmount())
                .isEqualByComparingTo("3000.00");

        assertThat(response.transactionCount())
                .isEqualTo(4L);
    }

    @Test
    void shouldReturnZeroWhenMonthlyAggregateValuesAreNull() {
        MonthlyAggregate aggregate =
                new MonthlyAggregate(
                        null,
                        null,
                        null
                );

        when(
                transactionProjectionRepository
                        .calculateMonthlyAggregate(
                                eq(1L),
                                any(Instant.class),
                                any(Instant.class),
                                eq(ProjectionTransactionType.INCOME),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(aggregate);

        MonthlyAnalyticsResponse response =
                analyticsService.getMonthlyAnalytics(
                        1L,
                        2026,
                        7,
                        CurrencyCode.TRY
                );

        assertThat(response.totalIncome())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.totalExpense())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.netAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.transactionCount())
                .isZero();
    }

    @Test
    void shouldCalculateDailyExpense() {
        List<DailyCashFlowAggregate> aggregates =
                List.of(
                        new DailyCashFlowAggregate(
                                1,
                                new BigDecimal("500.00"),
                                new BigDecimal("250.00")
                        ),
                        new DailyCashFlowAggregate(
                                5,
                                new BigDecimal("1000.00"),
                                new BigDecimal("750.00")
                        ),
                        new DailyCashFlowAggregate(
                                15,
                                new BigDecimal("2000.00"),
                                new BigDecimal("1000.00")
                        )
                );

        when(
                transactionProjectionRepository
                        .calculateDailyCashFlow(
                                eq(1L),
                                eq(
                                        Instant.parse(
                                                "2026-07-01T00:00:00Z"
                                        )
                                ),
                                eq(
                                        Instant.parse(
                                                "2026-08-01T00:00:00Z"
                                        )
                                ),
                                eq(ProjectionTransactionType.INCOME),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(aggregates);

        DailyCashFlowResponse response =
                analyticsService.getDailyExpense(
                        1L,
                        2026,
                        7,
                        CurrencyCode.TRY
                );

        assertThat(response.year())
                .isEqualTo(2026);

        assertThat(response.month())
                .isEqualTo(7);

        assertThat(response.totalExpense())
                .isEqualByComparingTo("2000.00");

        assertThat(response.days())
                .hasSize(3);

        assertThat(response.days().get(0).day())
                .isEqualTo(1);

        assertThat(response.days().get(0).totalExpense())
                .isEqualByComparingTo("250.00");

        assertThat(response.days().get(2).day())
                .isEqualTo(15);

        assertThat(response.days().get(2).totalExpense())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldReturnEmptyDailyExpenseWhenThereAreNoExpenses() {
        when(
                transactionProjectionRepository
                        .calculateDailyCashFlow(
                                eq(1L),
                                any(Instant.class),
                                any(Instant.class),
                                eq(ProjectionTransactionType.INCOME),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(List.of());

        DailyCashFlowResponse response =
                analyticsService.getDailyExpense(
                        1L,
                        2026,
                        7,
                        CurrencyCode.TRY
                );

        assertThat(response.totalExpense())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.days())
                .isEmpty();
    }

    @Test
    void shouldCalculateMonthlyCategoryAnalytics() {
        List<CategoryExpenseAggregate> aggregates =
                List.of(
                        new CategoryExpenseAggregate(
                                1L,
                                "Food",
                                new BigDecimal("1000.00"),
                                2L
                        ),
                        new CategoryExpenseAggregate(
                                2L,
                                "Travel",
                                new BigDecimal("500.00"),
                                1L
                        )
                );

        when(
                transactionProjectionRepository
                        .calculateCategoryExpenses(
                                eq(1L),
                                eq(
                                        Instant.parse(
                                                "2026-07-01T00:00:00Z"
                                        )
                                ),
                                eq(
                                        Instant.parse(
                                                "2026-08-01T00:00:00Z"
                                        )
                                ),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(aggregates);

        MonthlyCategoryAnalyticsResponse response =
                analyticsService
                        .getMonthlyCategoryAnalytics(
                                1L,
                                2026,
                                7,
                                CurrencyCode.TRY
                        );

        assertThat(response.year())
                .isEqualTo(2026);

        assertThat(response.month())
                .isEqualTo(7);

        assertThat(response.totalExpense())
                .isEqualByComparingTo("1500.00");

        assertThat(response.categories())
                .hasSize(2);

        assertThat(response.categories().get(0).categoryId())
                .isEqualTo(1L);

        assertThat(response.categories().get(0).categoryName())
                .isEqualTo("Food");

        assertThat(response.categories().get(0).totalExpense())
                .isEqualByComparingTo("1000.00");

        assertThat(response.categories().get(0).percentage())
                .isEqualByComparingTo("66.67");

        assertThat(response.categories().get(0).transactionCount())
                .isEqualTo(2L);

        assertThat(response.categories().get(1).categoryId())
                .isEqualTo(2L);

        assertThat(response.categories().get(1).percentage())
                .isEqualByComparingTo("33.33");
    }

    @Test
    void shouldReturnEmptyCategoryAnalyticsWhenThereAreNoExpenses() {
        when(
                transactionProjectionRepository
                        .calculateCategoryExpenses(
                                eq(1L),
                                any(Instant.class),
                                any(Instant.class),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(List.of());

        MonthlyCategoryAnalyticsResponse response =
                analyticsService
                        .getMonthlyCategoryAnalytics(
                                1L,
                                2026,
                                7,
                                CurrencyCode.TRY
                        );

        assertThat(response.totalExpense())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.categories())
                .isEmpty();
    }

    @Test
    void shouldReturnZeroPercentageWhenTotalExpenseIsZero() {
        List<CategoryExpenseAggregate> aggregates =
                List.of(
                        new CategoryExpenseAggregate(
                                1L,
                                "Food",
                                BigDecimal.ZERO,
                                0L
                        )
                );

        when(
                transactionProjectionRepository
                        .calculateCategoryExpenses(
                                eq(1L),
                                any(Instant.class),
                                any(Instant.class),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(aggregates);

        MonthlyCategoryAnalyticsResponse response =
                analyticsService
                        .getMonthlyCategoryAnalytics(
                                1L,
                                2026,
                                7,
                                CurrencyCode.TRY
                        );

        assertThat(response.totalExpense())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.categories().get(0).percentage())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnMonthlyTrendInChronologicalOrder() {
        MonthlyAggregate firstMonth =
                new MonthlyAggregate(
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00"),
                        2L
                );

        MonthlyAggregate secondMonth =
                new MonthlyAggregate(
                        new BigDecimal("2000.00"),
                        new BigDecimal("750.00"),
                        3L
                );

        MonthlyAggregate thirdMonth =
                new MonthlyAggregate(
                        new BigDecimal("3000.00"),
                        new BigDecimal("1000.00"),
                        4L
                );

        when(
                transactionProjectionRepository
                        .calculateMonthlyAggregate(
                                eq(1L),
                                any(Instant.class),
                                any(Instant.class),
                                eq(ProjectionTransactionType.INCOME),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(
                firstMonth,
                secondMonth,
                thirdMonth
        );

        MonthlyTrendResponse response =
                analyticsService.getMonthlyTrend(
                        1L,
                        3,
                        CurrencyCode.TRY
                );

        YearMonth currentMonth =
                YearMonth.now(ZoneOffset.UTC);

        YearMonth expectedFirstMonth =
                currentMonth.minusMonths(2);

        YearMonth expectedSecondMonth =
                currentMonth.minusMonths(1);

        assertThat(response.months())
                .hasSize(3);

        assertThat(response.months().get(0).year())
                .isEqualTo(expectedFirstMonth.getYear());

        assertThat(response.months().get(0).month())
                .isEqualTo(expectedFirstMonth.getMonthValue());

        assertThat(response.months().get(1).year())
                .isEqualTo(expectedSecondMonth.getYear());

        assertThat(response.months().get(1).month())
                .isEqualTo(expectedSecondMonth.getMonthValue());

        assertThat(response.months().get(2).year())
                .isEqualTo(currentMonth.getYear());

        assertThat(response.months().get(2).month())
                .isEqualTo(currentMonth.getMonthValue());

        assertThat(response.months().get(0).totalIncome())
                .isEqualByComparingTo("1000.00");

        assertThat(response.months().get(2).totalIncome())
                .isEqualByComparingTo("3000.00");
    }

    @Test
    void shouldCompareTwoMonthlyPeriods() {
        MonthlyAggregate baseAggregate =
                new MonthlyAggregate(
                        new BigDecimal("4000.00"),
                        new BigDecimal("2500.00"),
                        4L
                );

        MonthlyAggregate comparisonAggregate =
                new MonthlyAggregate(
                        new BigDecimal("5000.00"),
                        new BigDecimal("2000.00"),
                        5L
                );

        when(
                transactionProjectionRepository
                        .calculateMonthlyAggregate(
                                eq(1L),
                                any(Instant.class),
                                any(Instant.class),
                                eq(ProjectionTransactionType.INCOME),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(
                baseAggregate,
                comparisonAggregate
        );

        MonthlyComparisonResponse response =
                analyticsService.getMonthlyComparison(
                        1L,
                        2026,
                        5,
                        2026,
                        7,
                        CurrencyCode.TRY
                );

        assertThat(response.basePeriod().year())
                .isEqualTo(2026);

        assertThat(response.basePeriod().month())
                .isEqualTo(5);

        assertThat(response.comparisonPeriod().year())
                .isEqualTo(2026);

        assertThat(response.comparisonPeriod().month())
                .isEqualTo(7);

        assertThat(response.incomeChangePercentage())
                .isEqualByComparingTo("25.00");

        assertThat(response.expenseChangePercentage())
                .isEqualByComparingTo("-20.00");
    }

    @Test
    void shouldReturnZeroChangeWhenBasePeriodValuesAreZero() {
        MonthlyAggregate baseAggregate =
                new MonthlyAggregate(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0L
                );

        MonthlyAggregate comparisonAggregate =
                new MonthlyAggregate(
                        new BigDecimal("5000.00"),
                        new BigDecimal("2000.00"),
                        2L
                );

        when(
                transactionProjectionRepository
                        .calculateMonthlyAggregate(
                                eq(1L),
                                any(Instant.class),
                                any(Instant.class),
                                eq(ProjectionTransactionType.INCOME),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(
                baseAggregate,
                comparisonAggregate
        );

        MonthlyComparisonResponse response =
                analyticsService.getMonthlyComparison(
                        1L,
                        2026,
                        6,
                        2026,
                        7,
                        CurrencyCode.TRY
                );

        assertThat(response.incomeChangePercentage())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.expenseChangePercentage())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCalculateYearlyAnalytics() {
        MonthlyAggregate aggregate =
                new MonthlyAggregate(
                        new BigDecimal("60000.00"),
                        new BigDecimal("36000.00"),
                        120L
                );

        when(
                transactionProjectionRepository
                        .calculateMonthlyAggregate(
                                eq(1L),
                                eq(
                                        Instant.parse(
                                                "2026-01-01T00:00:00Z"
                                        )
                                ),
                                eq(
                                        Instant.parse(
                                                "2027-01-01T00:00:00Z"
                                        )
                                ),
                                eq(ProjectionTransactionType.INCOME),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(aggregate);

        YearlyAnalyticsResponse response =
                analyticsService.getYearlyAnalytics(
                        1L,
                        2026,
                        CurrencyCode.TRY
                );

        assertThat(response.year())
                .isEqualTo(2026);

        assertThat(response.totalIncome())
                .isEqualByComparingTo("60000.00");

        assertThat(response.totalExpense())
                .isEqualByComparingTo("36000.00");

        assertThat(response.netAmount())
                .isEqualByComparingTo("24000.00");

        assertThat(response.transactionCount())
                .isEqualTo(120L);

        assertThat(response.averageMonthlyExpense())
                .isEqualByComparingTo("3000.00");
    }

    @Test
    void shouldReturnZeroWhenYearlyAggregateValuesAreNull() {
        MonthlyAggregate aggregate =
                new MonthlyAggregate(
                        null,
                        null,
                        null
                );

        when(
                transactionProjectionRepository
                        .calculateMonthlyAggregate(
                                eq(1L),
                                any(Instant.class),
                                any(Instant.class),
                                eq(ProjectionTransactionType.INCOME),
                                eq(ProjectionTransactionType.EXPENSE),
                                eq(CurrencyCode.TRY)
                        )
        ).thenReturn(aggregate);

        YearlyAnalyticsResponse response =
                analyticsService.getYearlyAnalytics(
                        1L,
                        2026,
                        CurrencyCode.TRY
                );

        assertThat(response.totalIncome())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.totalExpense())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.netAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.transactionCount())
                .isZero();

        assertThat(response.averageMonthlyExpense())
                .isEqualByComparingTo("0.00");
    }
}