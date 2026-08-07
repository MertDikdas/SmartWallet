package com.smartwallet.budgetservice.repository;

import com.smartwallet.budgetservice.entity.MonthlyCategorySpending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface MonthlyCategorySpendingRepository extends JpaRepository<MonthlyCategorySpending, Integer> {

    Optional<MonthlyCategorySpending> findByUserIdAndCategoryIdAndYearAndMonth(
            Long userId,
            Long categoryId,
            Integer year,
            Integer month
    );

    Optional<MonthlyCategorySpending> findMonthlyCategorySpendingByUserIdAndCategoryIdAndYearAndMonth(
            Long userId,
            Long categoryId,
            Integer year,
            Integer month
    );

}
