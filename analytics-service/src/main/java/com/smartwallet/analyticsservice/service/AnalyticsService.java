package com.smartwallet.analyticsservice.service;

import com.smartwallet.analyticsservice.dto.projection.CategoryExpenseAggregate;
import com.smartwallet.analyticsservice.dto.projection.DailyCashFlowAggregate;
import com.smartwallet.analyticsservice.dto.projection.MonthlyAggregate;
import com.smartwallet.analyticsservice.dto.response.*;
import com.smartwallet.analyticsservice.entity.ProjectionTransactionType;
import com.smartwallet.analyticsservice.repository.TransactionProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionProjectionRepository
            transactionProjectionRepository;

    @Transactional(readOnly = true)
    public MonthlyAnalyticsResponse getMonthlyAnalytics(
            Long userId,
            int year,
            int month
    ) {
        YearMonth period = YearMonth.of(year, month);

        Instant startDate =
                period.atDay(1)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();

        Instant endDate =
                period.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();

        MonthlyAggregate aggregate =
                transactionProjectionRepository
                        .calculateMonthlyAggregate(
                                userId,
                                startDate,
                                endDate,
                                ProjectionTransactionType.INCOME,
                                ProjectionTransactionType.EXPENSE
                        );

        BigDecimal totalIncome =
                aggregate.totalIncome() != null
                        ? aggregate.totalIncome()
                        : BigDecimal.ZERO;

        BigDecimal totalExpense =
                aggregate.totalExpense() != null
                        ? aggregate.totalExpense()
                        : BigDecimal.ZERO;

        long transactionCount =
                aggregate.transactionCount() != null
                        ? aggregate.transactionCount()
                        : 0L;

        return new MonthlyAnalyticsResponse(
                year,
                month,
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                transactionCount
        );
    }

    @Transactional(readOnly = true)
    public DailyCashFlowResponse getDailyExpense(
            Long userId,
            int year,
            int month
    ) {
        YearMonth period = YearMonth.of(year, month);

        Instant startDate = period
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant endDate = period
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        List<DailyCashFlowAggregate> aggregates =
                transactionProjectionRepository
                        .calculateDailyCashFlow(
                                userId,
                                startDate,
                                endDate,
                                ProjectionTransactionType.INCOME,
                                ProjectionTransactionType.EXPENSE
                        );

        BigDecimal totalExpense = aggregates.stream()
                .map(DailyCashFlowAggregate::totalExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIncome = aggregates.stream()
                .map(DailyCashFlowAggregate::totalIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DailyCashFlowItemResponse> days = aggregates.stream()
                .map(aggregate ->
                        new DailyCashFlowItemResponse(
                                aggregate.day(),
                                aggregate.totalIncome(),
                                aggregate.totalExpense()
                        )
                )
                .toList();

        return new DailyCashFlowResponse(
                year,
                month,
                totalIncome,
                totalExpense,
                days
        );
    }

    @Transactional(readOnly = true)
    public MonthlyCategoryAnalyticsResponse getMonthlyCategoryAnalytics(
            Long userId,
            int year,
            int month
    ){
        YearMonth period = YearMonth.of(year, month);

        Instant startDate = period
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant endDate = period
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        List<CategoryExpenseAggregate> aggregates =
                transactionProjectionRepository
                        .calculateCategoryExpenses(
                                userId,
                                startDate,
                                endDate,
                                ProjectionTransactionType.EXPENSE
                        );
        BigDecimal totalExpense = aggregates.stream()
                .map(aggregate -> aggregate.totalExpense())
                .reduce(
                        BigDecimal.ZERO,
                        (a,b) -> a.add(b)
                );
        List<CategoryExpenseResponse> categories = aggregates.stream()
                .map(aggregate ->
                        new CategoryExpenseResponse(
                                aggregate.categoryId(),
                                aggregate.categoryName(),
                                aggregate.totalExpense(),
                                calculatePercentage(
                                        aggregate.totalExpense(),
                                        totalExpense
                                ),
                                aggregate.transactionCount()
                        )
                )
                .toList();
        return new MonthlyCategoryAnalyticsResponse(
                year,
                month,
                totalExpense,
                categories
        );
    }

    @Transactional(readOnly = true)
    public MonthlyTrendResponse getMonthlyTrend(
            Long userId,
            int months
    ) {
        YearMonth currentMonth =
                YearMonth.now(ZoneOffset.UTC);

        List<MonthlyTrendItemResponse> trendItems =
                new ArrayList<>();

        for (int offset = months - 1; offset >= 0; offset--) {

            YearMonth period =
                    currentMonth.minusMonths(offset);

            MonthlyAnalyticsResponse monthlyAnalytics =
                    getMonthlyAnalytics(
                            userId,
                            period.getYear(),
                            period.getMonthValue()
                    );

            MonthlyTrendItemResponse trendItem =
                    new MonthlyTrendItemResponse(
                            monthlyAnalytics.year(),
                            monthlyAnalytics.month(),
                            monthlyAnalytics.totalIncome(),
                            monthlyAnalytics.totalExpense(),
                            monthlyAnalytics.netAmount(),
                            monthlyAnalytics.transactionCount()
                    );

            trendItems.add(trendItem);
        }

        return new MonthlyTrendResponse(trendItems);
    }

    @Transactional(readOnly = true)
    public MonthlyComparisonResponse getMonthlyComparison(
            Long userId,
            int baseYear,
            int baseMonth,
            int comparisonYear,
            int comparisonMonth
    ) {
        MonthlyAnalyticsResponse baseAnalytics =
                getMonthlyAnalytics(
                        userId,
                        baseYear,
                        baseMonth
                );

        MonthlyAnalyticsResponse comparisonAnalytics =
                getMonthlyAnalytics(
                        userId,
                        comparisonYear,
                        comparisonMonth
                );

        MonthlyComparisonItemResponse basePeriod =
                new MonthlyComparisonItemResponse(
                        baseAnalytics.year(),
                        baseAnalytics.month(),
                        baseAnalytics.totalIncome(),
                        baseAnalytics.totalExpense(),
                        baseAnalytics.netAmount()
                );

        MonthlyComparisonItemResponse comparisonPeriod =
                new MonthlyComparisonItemResponse(
                        comparisonAnalytics.year(),
                        comparisonAnalytics.month(),
                        comparisonAnalytics.totalIncome(),
                        comparisonAnalytics.totalExpense(),
                        comparisonAnalytics.netAmount()
                );

        BigDecimal incomeChangePercentage =
                calculateChangePercentage(
                        comparisonAnalytics.totalIncome(),
                        baseAnalytics.totalIncome()
                );

        BigDecimal expenseChangePercentage =
                calculateChangePercentage(
                        comparisonAnalytics.totalExpense(),
                        baseAnalytics.totalExpense()
                );

        return new MonthlyComparisonResponse(
                basePeriod,
                comparisonPeriod,
                incomeChangePercentage,
                expenseChangePercentage
        );
    }

    @Transactional(readOnly = true)
    public YearlyAnalyticsResponse getYearlyAnalytics(
            Long userId,
            int year
    ) {
        Year period = Year.of(year);

        Instant startDate = period
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant endDate = period
                .plusYears(1)
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        MonthlyAggregate aggregate =
                transactionProjectionRepository
                        .calculateMonthlyAggregate(
                                userId,
                                startDate,
                                endDate,
                                ProjectionTransactionType.INCOME,
                                ProjectionTransactionType.EXPENSE
                        );

        BigDecimal totalIncome =
                aggregate.totalIncome() != null
                        ? aggregate.totalIncome()
                        : BigDecimal.ZERO;

        BigDecimal totalExpense =
                aggregate.totalExpense() != null
                        ? aggregate.totalExpense()
                        : BigDecimal.ZERO;

        long transactionCount =
                aggregate.transactionCount() != null
                        ? aggregate.transactionCount()
                        : 0L;

        BigDecimal netAmount =
                totalIncome.subtract(totalExpense);

        BigDecimal averageMonthlyExpense =
                totalExpense.divide(
                        BigDecimal.valueOf(12),
                        2,
                        RoundingMode.HALF_UP
                );

        return new YearlyAnalyticsResponse(
                year,
                totalIncome,
                totalExpense,
                netAmount,
                transactionCount,
                averageMonthlyExpense
        );
    }

    private BigDecimal calculatePercentage(
            BigDecimal categoryExpense,
            BigDecimal totalExpense
    ) {
        if (totalExpense.signum() == 0) {
            return BigDecimal.ZERO;
        }

        return categoryExpense
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        totalExpense,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateChangePercentage(
            BigDecimal comparisonValue,
            BigDecimal baseValue
    ) {
        if (baseValue.signum() == 0) {
            return BigDecimal.ZERO;
        }

        return comparisonValue
                .subtract(baseValue)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        baseValue,
                        2,
                        RoundingMode.HALF_UP
                );
    }


}