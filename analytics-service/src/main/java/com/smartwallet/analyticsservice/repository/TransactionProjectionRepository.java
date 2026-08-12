package com.smartwallet.analyticsservice.repository;

import com.smartwallet.analyticsservice.dto.projection.CategoryExpenseAggregate;
import com.smartwallet.analyticsservice.dto.projection.DailyCashFlowAggregate;
import com.smartwallet.analyticsservice.dto.projection.MonthlyAggregate;
import com.smartwallet.analyticsservice.entity.ProjectionTransactionType;
import com.smartwallet.analyticsservice.entity.TransactionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TransactionProjectionRepository
        extends JpaRepository<TransactionProjection, Long> {

    @Query("""
            SELECT new com.smartwallet.analyticsservice.dto.projection.MonthlyAggregate(
                SUM(
                    CASE
                        WHEN projection.transactionType = :incomeType
                        THEN projection.amount
                        ELSE 0
                    END
                ),
                SUM(
                    CASE
                        WHEN projection.transactionType = :expenseType
                        THEN projection.amount
                        ELSE 0
                    END
                ),
                COUNT(projection)
            )
            FROM TransactionProjection projection
            WHERE projection.userId = :userId
              AND projection.transactionDate >= :startDate
              AND projection.transactionDate < :endDate
            """)
    MonthlyAggregate calculateMonthlyAggregate(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("incomeType")
            ProjectionTransactionType incomeType,
            @Param("expenseType")
            ProjectionTransactionType expenseType
    );

    @Query("""
        SELECT new com.smartwallet.analyticsservice.dto.projection.CategoryExpenseAggregate(
            projection.categoryId,
            projection.categoryName,
            SUM(projection.amount),
            COUNT(projection)
        )
        FROM TransactionProjection projection
        WHERE projection.userId = :userId
          AND projection.transactionType = :expenseType
          AND projection.transactionDate >= :startDate
          AND projection.transactionDate < :endDate
        GROUP BY projection.categoryId,
                projection.categoryName
        ORDER BY SUM(projection.amount) DESC
        """)
    List<CategoryExpenseAggregate> calculateCategoryExpenses(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("expenseType")
            ProjectionTransactionType expenseType
    );

    @Query("""
    SELECT new com.smartwallet.analyticsservice.dto.projection.DailyCashFlowAggregate(
        day(projection.transactionDate),

        SUM(
            CASE
                WHEN projection.transactionType = :incomeType
                THEN projection.amount
                ELSE 0
            END
        ),

        SUM(
            CASE
                WHEN projection.transactionType = :expenseType
                THEN projection.amount
                ELSE 0
            END
        )
    )
    FROM TransactionProjection projection
    WHERE projection.userId = :userId
      AND projection.transactionDate >= :startDate
      AND projection.transactionDate < :endDate
      AND projection.transactionType IN (:incomeType, :expenseType)
    GROUP BY day(projection.transactionDate)
    ORDER BY day(projection.transactionDate)
    """)
    List<DailyCashFlowAggregate> calculateDailyCashFlow(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("incomeType") ProjectionTransactionType incomeType,
            @Param("expenseType") ProjectionTransactionType expenseType
    );
}
