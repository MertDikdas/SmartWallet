package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.client.BudgetClient;
import com.smartwallet.financeservice.dto.request.CreateCategoryRequest;
import com.smartwallet.financeservice.dto.response.CategoryResponse;
import com.smartwallet.financeservice.entity.Category;
import com.smartwallet.financeservice.exception.BudgetFoundForCategoryException;
import com.smartwallet.financeservice.exception.CategoryAlreadyExistsException;
import com.smartwallet.financeservice.exception.CategoryCannotDeleteBecauseTransactions;
import com.smartwallet.financeservice.exception.CategoryNotFoundException;
import com.smartwallet.financeservice.mapper.CategoryMapper;
import com.smartwallet.financeservice.repository.CategoryRepository;
import com.smartwallet.financeservice.repository.FinancialTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final BudgetClient budgetClient;

    @Transactional
    public CategoryResponse createCategory(
            Long userId,
            CreateCategoryRequest request
    ) {
        String normalizedName = request.name().trim();

        boolean categoryExists =
                categoryRepository
                        .existsByUserIdAndNameIgnoreCaseAndType(
                                userId,
                                normalizedName,
                                request.type()
                        );

        if (categoryExists) {
            throw new CategoryAlreadyExistsException(
                    normalizedName
            );
        }

        Category category = Category.builder()
                .userId(userId)
                .name(normalizedName)
                .type(request.type())
                .build();

        Category savedCategory =
                categoryRepository.save(category);

        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Long userId) {
        return categoryRepository
                .findAllByUserIdOrderByNameAsc(userId)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(
            Long userId,
            Long categoryId
    ) {
        Category category = categoryRepository
                .findByIdAndUserId(categoryId, userId)
                .orElseThrow(
                        () -> new CategoryNotFoundException(categoryId)
                );

        return categoryMapper.toResponse(category);
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId, String accessToken){
        Category category = categoryRepository
                .findByIdAndUserId(categoryId, userId)
                .orElseThrow(
                        () -> new CategoryNotFoundException(categoryId)
                );

        boolean transactionExists =
                financialTransactionRepository
                        .existsByUserIdAndCategory(
                                userId,
                                category
                        );

        if(transactionExists){
            throw new CategoryCannotDeleteBecauseTransactions();
        }

        Boolean isBudgetExists =
                budgetClient
                        .getCategoryBudget(
                                categoryId,
                                accessToken
                        );

        if(isBudgetExists){
            throw new BudgetFoundForCategoryException();
        }

        categoryRepository.delete(category);
    }
}