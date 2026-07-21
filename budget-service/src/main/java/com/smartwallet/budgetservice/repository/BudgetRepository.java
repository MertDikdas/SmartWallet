package com.smartwallet.budgetservice.repository;

import com.smartwallet.budgetservice.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

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

    boolean existsByUserIdAndCategoryIdAndYearAndMonth(
            Long userId,
            Long categoryId,
            Integer year,
            Integer month
    );
}