package com.smartwallet.analyticsservice.service;

import com.smartwallet.analyticsservice.dto.projection.CategoryExpenseAggregate;
import com.smartwallet.analyticsservice.dto.projection.MonthlyAggregate;
import com.smartwallet.analyticsservice.dto.response.CategoryExpenseResponse;
import com.smartwallet.analyticsservice.dto.response.MonthlyAnalyticsResponse;
import com.smartwallet.analyticsservice.dto.response.MonthlyCategoryAnalyticsResponse;
import com.smartwallet.analyticsservice.entity.ProjectionTransactionType;
import com.smartwallet.analyticsservice.repository.TransactionProjectionRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
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
}