package com.smartwallet.budgetservice.repository;

import com.smartwallet.budgetservice.entity.CurrencyCode;
import com.smartwallet.budgetservice.entity.MonthlyCategorySpending;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MonthlyCategorySpendingRepository extends JpaRepository<MonthlyCategorySpending, Integer> {

    Optional<MonthlyCategorySpending> findByUserIdAndCategoryIdAndYearAndMonthAndCurrency(
            Long userId,
            Long categoryId,
            Integer year,
            Integer month,
            CurrencyCode currency
    );

    Optional<MonthlyCategorySpending> findMonthlyCategorySpendingByUserIdAndCategoryIdAndYearAndMonthAndCurrency(
            Long userId,
            Long categoryId,
            Integer year,
            Integer month,
            CurrencyCode currency
    );

}
