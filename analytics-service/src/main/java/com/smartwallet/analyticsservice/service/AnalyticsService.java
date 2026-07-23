package com.smartwallet.analyticsservice.service;

import com.smartwallet.analyticsservice.dto.projection.MonthlyAggregate;
import com.smartwallet.analyticsservice.dto.response.MonthlyAnalyticsResponse;
import com.smartwallet.analyticsservice.entity.ProjectionTransactionType;
import com.smartwallet.analyticsservice.repository.TransactionProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

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
}