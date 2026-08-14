package com.smartwallet.budgetservice.repository;

import com.smartwallet.budgetservice.entity.Budget;
import com.smartwallet.budgetservice.entity.CurrencyCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository
        extends JpaRepository<Budget, Long> {

    List<Budget> findAllByUserIdOrderByYearDescMonthDesc(
            Long userId
    );

    Optional<Budget> findByIdAndUserId(
            Long budgetId,
            Long userId
    );

    boolean existsByUserIdAndCategoryIdAndYearAndMonthAndCurrency(
            Long userId,
            Long categoryId,
            Integer year,
            Integer month,
            CurrencyCode currency
    );

    Boolean existsByUserIdAndCategoryId(
            Long userId,
            Long categoryId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT budget
        FROM Budget budget
        WHERE budget.userId = :userId
          AND budget.categoryId = :categoryId
          AND budget.year = :year
          AND budget.month = :month
          AND budget.currency = :currency
        """)
    Optional<Budget> findForUpdate(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("currency") CurrencyCode currency
    );
}