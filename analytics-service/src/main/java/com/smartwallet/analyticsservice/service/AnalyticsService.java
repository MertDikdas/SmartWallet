package com.smartwallet.analyticsservice.service;

import com.smartwallet.analyticsservice.dto.projection.CategoryExpenseAggregate;
import com.smartwallet.analyticsservice.dto.projection.MonthlyAggregate;
import com.smartwallet.analyticsservice.dto.response.*;
import com.smartwallet.analyticsservice.dto.response.MonthlyAnalyticsResponse;
import com.smartwallet.analyticsservice.entity.ProjectionTransactionType;
import com.smartwallet.analyticsservice.repository.TransactionProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionProjectionRepository
            transactionProjectionRepository;
    private final SecurityExpressionHandler securityExpressionHandler;

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
                .map(CategoryExpenseAggregate::totalExpense)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
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
    public MonthlyComparisonResponse getMonthlyComparision(
            Long userId,
            int baseYear,
            int baseMonth,
            int comparisonYear,
            int comparisonMonth
    ){
        YearMonth currentMonth = YearMonth.of(baseYear, baseMonth);
        YearMonth previousMonth = YearMonth.of(comparisonYear, comparisonMonth);
        MonthlyAnalyticsResponse currentMonthlyAnalytics =
                getMonthlyAnalytics(
                        userId,
                        baseYear,
                        baseMonth
                );

        MonthlyComparisonItemResponse currentItem = new MonthlyComparisonItemResponse(
                baseYear,
                baseMonth,
                currentMonthlyAnalytics.totalIncome(),
                currentMonthlyAnalytics.totalExpense(),
                currentMonthlyAnalytics.netAmount()

        );
        MonthlyAnalyticsResponse previousMonthlyAnalytics =
                getMonthlyAnalytics(
                        userId,
                        comparisonYear,
                        comparisonMonth
                );
        MonthlyComparisonItemResponse previousItem = new MonthlyComparisonItemResponse(
                comparisonYear,
                comparisonMonth,
                previousMonthlyAnalytics.totalIncome(),
                previousMonthlyAnalytics.totalExpense(),
                previousMonthlyAnalytics.netAmount()
        );
        BigDecimal incomeChangePercentage  = 
                calculateChangePrecentage(
                        currentMonthlyAnalytics.totalIncome(), 
                        previousMonthlyAnalytics.totalIncome()
                );
        BigDecimal expenseChangePercentage = calculateChangePrecentage(
                currentMonthlyAnalytics.totalExpense(),
                previousMonthlyAnalytics.totalExpense()
        );
        
        MonthlyComparisonResponse monthlyComparisonResponse = new MonthlyComparisonResponse(
                currentItem,
                previousItem,
                incomeChangePercentage,
                expenseChangePercentage
        );
        return monthlyComparisonResponse;
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
    
    private BigDecimal calculateChangePrecentage(
            BigDecimal base,
            BigDecimal previous
    ){
        if(previous.signum() == 0){
            return BigDecimal.ZERO;
        }
        return base.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP);
    }
}