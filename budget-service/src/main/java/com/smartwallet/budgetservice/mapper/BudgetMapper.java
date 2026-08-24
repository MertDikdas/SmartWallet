package com.smartwallet.budgetservice.mapper;

import com.smartwallet.budgetservice.dto.response.BudgetResponse;
import com.smartwallet.budgetservice.entity.Budget;
import org.springframework.stereotype.Component;

@Component
public class BudgetMapper {

    public BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCategoryId(),
                budget.getLimitAmount(),
                budget.getSpentAmount(),
                budget.getLimitAmount()
                        .subtract(budget.getSpentAmount()),
                budget.getYear(),
                budget.getMonth(),
                budget.getStatus(),
                budget.getCurrency(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}