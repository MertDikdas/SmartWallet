package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.CreateTransactionRequest;
import com.smartwallet.financeservice.dto.response.TransactionResponse;
import com.smartwallet.financeservice.entity.*;
import com.smartwallet.financeservice.exception.AccountNotFoundException;
import com.smartwallet.financeservice.exception.CategoryNotFoundException;
import com.smartwallet.financeservice.exception.CategoryTypeMismatchException;
import com.smartwallet.financeservice.mapper.TransactionMapper;
import com.smartwallet.financeservice.repository.AccountRepository;
import com.smartwallet.financeservice.repository.CategoryRepository;
import com.smartwallet.financeservice.repository.FinancialTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse createTransaction(
            Long userId,
            CreateTransactionRequest request
    ) {
        Account account = accountRepository
                .findOwnedAccountForUpdate(
                        request.accountId(),
                        userId
                )
                .orElseThrow(
                        () -> new AccountNotFoundException(
                                request.accountId()
                        )
                );

        Category category = categoryRepository
                .findByIdAndUserId(
                        request.categoryId(),
                        userId
                )
                .orElseThrow(
                        () -> new CategoryNotFoundException(
                                request.categoryId()
                        )
                );

        validateCategoryType(category, request.type());

        BigDecimal newBalance = switch (request.type()) {
            case INCOME ->
                    account.getBalance().add(request.amount());

            case EXPENSE ->
                    account.getBalance().subtract(request.amount());
        };

        account.setBalance(newBalance);

        // Bakiyeyi açıkça veritabanına kaydet
        accountRepository.save(account);

        FinancialTransaction transaction =
                FinancialTransaction.builder()
                        .userId(userId)
                        .account(account)
                        .category(category)
                        .type(request.type())
                        .amount(request.amount())
                        .description(
                                normalizeDescription(request.description())
                        )
                        .transactionDate(
                                request.transactionDate() != null
                                        ? request.transactionDate()
                                        : Instant.now()
                        )
                        .build();

        FinancialTransaction savedTransaction =
                transactionRepository.save(transaction);

        return transactionMapper.toResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(
            Long userId
    ) {
        return transactionRepository
                .findAllByUserIdOrderByTransactionDateDesc(userId)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    private void validateCategoryType(
            Category category,
            TransactionType transactionType
    ) {
        if (category.getType() != transactionType) {
            throw new CategoryTypeMismatchException();
        }
    }

    private void updateAccountBalance(
            Account account,
            TransactionType type,
            BigDecimal amount
    ) {
        BigDecimal currentBalance = account.getBalance();

        BigDecimal newBalance = switch (type) {
            case INCOME -> currentBalance.add(amount);
            case EXPENSE -> currentBalance.subtract(amount);
        };

        account.setBalance(newBalance);
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();

        return normalized.isEmpty() ? null : normalized;
    }
}