package com.smartwallet.budgetservice.service;

import com.smartwallet.budgetservice.client.CategoryClient;
import com.smartwallet.budgetservice.client.dto.CategoryDetailsResponse;
import com.smartwallet.budgetservice.dto.request.CreateBudgetRequest;
import com.smartwallet.budgetservice.dto.request.UpdateBudgetRequest;
import com.smartwallet.budgetservice.dto.response.BudgetResponse;
import com.smartwallet.budgetservice.entity.Budget;
import com.smartwallet.budgetservice.entity.BudgetStatus;
import com.smartwallet.budgetservice.exception.BudgetAlreadyExistsException;
import com.smartwallet.budgetservice.exception.BudgetNotFoundException;
import com.smartwallet.budgetservice.exception.InvalidBudgetCategoryException;
import com.smartwallet.budgetservice.mapper.BudgetMapper;
import com.smartwallet.budgetservice.outbox.BudgetEventOutboxService;
import com.smartwallet.budgetservice.repository.BudgetRepository;
import com.smartwallet.budgetservice.repository.MonthlyCategorySpendingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final BudgetEventOutboxService budgetEventOutboxService;
    private final CategoryClient categoryClient;

    private final MonthlyCategorySpendingRepository spendingRepository;

    @Transactional
    public BudgetResponse createBudget(
            Long userId,
            String accessToken,
            CreateBudgetRequest request
    ) {
        CategoryDetailsResponse category =
                categoryClient.getOwnedCategory(
                        request.categoryId(),
                        accessToken
                );

        if (!"EXPENSE".equals(category.type())) {

            throw new InvalidBudgetCategoryException(
                    request.categoryId()
            );
        }

        boolean exists =
                budgetRepository
                        .existsByUserIdAndCategoryIdAndYearAndMonthAndCurrency(
                                userId,
                                request.categoryId(),
                                request.year(),
                                request.month(),
                                request.currency()
                        );

        if (exists) {
            throw new BudgetAlreadyExistsException(
                    request.categoryId(),
                    request.year(),
                    request.month()
            );
        }


        Budget budget = Budget.builder()
                .userId(userId)
                .categoryId(request.categoryId())
                .limitAmount(request.limitAmount())
                .spentAmount(BigDecimal.ZERO)
                .year(request.year())
                .month(request.month())
                .currency(request.currency())
                .status(BudgetStatus.ACTIVE)
                .build();

        spendingRepository.findMonthlyCategorySpendingByUserIdAndCategoryIdAndYearAndMonthAndCurrency(
                userId,
                request.categoryId(),
                request.year(),
                request.month(),
                request.currency()
        ).ifPresent(spending -> {
            budget.setSpentAmount(spending.getSpentAmount());
            budget.recalculateStatus();
        });

        Budget savedBudget =
                budgetRepository.save(budget);

        return budgetMapper.toResponse(savedBudget);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(Long userId) {
        return budgetRepository
                .findAllByUserIdOrderByYearDescMonthDesc(userId)
                .stream()
                .map(budgetMapper::toResponse)
                .toList();
    }

    @Transactional
    public Boolean getBudgetByCategory(
            Long userId,
            Long categoryId
    ){
        return budgetRepository
                .existsByUserIdAndCategoryId(
                        userId,
                        categoryId
                );
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudget(
            Long userId,
            Long budgetId
    ) {
        Budget budget = findOwnedBudget(userId, budgetId);

        return budgetMapper.toResponse(budget);
    }

    @Transactional
    public BudgetResponse updateBudget(
            Long userId,
            Long budgetId,
            UpdateBudgetRequest request
    ) {
        Budget budget = findOwnedBudget(userId, budgetId);

        BudgetStatus previousStatus =
                budget.getStatus();

        budget.setLimitAmount(request.limitAmount());
        budget.recalculateStatus();

        budgetEventOutboxService.enqueueExceededIfNeeded(
                previousStatus,
                budget
        );

        Budget savedBudget =
                budgetRepository.save(budget);

        return budgetMapper.toResponse(savedBudget);
    }

    @Transactional
    public void deleteBudget(
            Long userId,
            Long budgetId
    ) {
        Budget budget = findOwnedBudget(userId, budgetId);

        budgetRepository.delete(budget);
    }

    private Budget findOwnedBudget(
            Long userId,
            Long budgetId
    ) {
        return budgetRepository
                .findByIdAndUserId(budgetId, userId)
                .orElseThrow(
                        () -> new BudgetNotFoundException(budgetId)
                );
    }
}