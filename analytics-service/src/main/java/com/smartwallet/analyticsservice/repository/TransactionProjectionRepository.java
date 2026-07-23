package com.smartwallet.analyticsservice.repository;

import com.smartwallet.analyticsservice.dto.projection.MonthlyAggregate;
import com.smartwallet.analyticsservice.entity.ProjectionTransactionType;
import com.smartwallet.analyticsservice.entity.TransactionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

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
}