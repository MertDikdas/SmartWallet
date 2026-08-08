package com.smartwallet.financeservice.service;

import com.smartwallet.contracts.transaction.TransactionChangedEvent;
import com.smartwallet.contracts.transaction.TransactionSnapshot;
import com.smartwallet.financeservice.dto.request.CreateTransactionRequest;
import com.smartwallet.financeservice.dto.request.UpdateTransactionRequest;
import com.smartwallet.financeservice.dto.response.TransactionResponse;
import com.smartwallet.financeservice.entity.*;
import com.smartwallet.financeservice.exception.AccountNotFoundException;
import com.smartwallet.financeservice.exception.CategoryNotFoundException;
import com.smartwallet.financeservice.exception.CategoryTypeMismatchException;
import com.smartwallet.financeservice.exception.FinancialTransactionNotFoundException;
import com.smartwallet.financeservice.mapper.TransactionMapper;
import com.smartwallet.financeservice.outbox.OutboxEventService;
import com.smartwallet.financeservice.repository.AccountRepository;
import com.smartwallet.financeservice.repository.CategoryRepository;
import com.smartwallet.financeservice.repository.FinancialTransactionRepository;
import com.smartwallet.financeservice.dto.request.TransactionFilterRequest;
import com.smartwallet.financeservice.dto.response.PageResponse;
import com.smartwallet.financeservice.specification.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;
    private final OutboxEventService outboxEventService;

    @Transactional
    public TransactionResponse createTransaction(
            Long userId,
            CreateTransactionRequest request
    ) {
        Account account = accountRepository
                .findOwnedAccountForUpdate(
                        request.accountId(),
                        userId,
                        AccountStatus.ACTIVE
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

        outboxEventService.enqueue(
                TransactionChangedEvent.created(
                        toSnapshot(savedTransaction)
                )
        );

        return transactionMapper.toResponse(savedTransaction);
    }


    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactions(
            Long userId,
            TransactionFilterRequest filter
    ){
        @SuppressWarnings("SPRING_DATA_STRING_PROPERTY_REFERENCE")
        Pageable pageable = PageRequest.of(
                filter.resolvedPage(),
                filter.resolvedSize(),
                Sort.by(
                        Sort.Order.desc("transactioDate"),
                        Sort.Order.desc("id")
        );

        Page<TransactionResponse> transactionPage =
                transactionRepository
                        .findAll(
                                TransactionSpecification.withFilters(
                                        userId,
                                        filter
                                ),
                                pageable
                        )
                        .map(transactionMapper::toResponse);

        return PageResponse.from(transactionPage);
    }

    @Transactional
    public TransactionResponse updateTransaction(
            Long userId,
            Long transactionId,
            UpdateTransactionRequest request
    ) {
        FinancialTransaction transaction =
                transactionRepository
                        .findOwnedTransactionForUpdate(
                                transactionId,
                                userId
                        )
                        .orElseThrow(
                                () -> new FinancialTransactionNotFoundException(
                                        transactionId
                                )
                        );
        TransactionSnapshot beforeSnapshot =
                toSnapshot(transaction);

        Long oldAccountId =
                transaction.getAccount().getId();

        Long newAccountId =
                request.accountId() != null
                        ? request.accountId()
                        : oldAccountId;

        AccountPair accounts = lockAccounts(
                userId,
                oldAccountId,
                newAccountId
        );

        Category newCategory =
                request.categoryId() != null
                        ? findOwnedCategory(
                        userId,
                        request.categoryId()
                )
                        : transaction.getCategory();

        TransactionType newType =
                request.type() != null
                        ? request.type()
                        : transaction.getType();

        BigDecimal newAmount =
                request.amount() != null
                        ? request.amount()
                        : transaction.getAmount();

        validateCategoryType(newCategory, newType);

        // Eski transaction'ın bakiye etkisini kaldır
        reverseBalanceEffect(
                accounts.oldAccount(),
                transaction.getType(),
                transaction.getAmount()
        );

        // Yeni transaction'ın bakiye etkisini uygula
        applyBalanceEffect(
                accounts.newAccount(),
                newType,
                newAmount
        );

        transaction.setAccount(accounts.newAccount());
        transaction.setCategory(newCategory);
        transaction.setType(newType);
        transaction.setAmount(newAmount);

        if (request.description() != null) {
            transaction.setDescription(
                    normalizeDescription(request.description())
            );
        }

        if (request.transactionDate() != null) {
            transaction.setTransactionDate(
                    request.transactionDate()
            );
        }

        accountRepository.save(accounts.oldAccount());

        if (!oldAccountId.equals(newAccountId)) {
            accountRepository.save(accounts.newAccount());
        }


        FinancialTransaction savedTransaction =
                transactionRepository.save(transaction);

        TransactionSnapshot afterSnapshot =
                toSnapshot(savedTransaction);

        outboxEventService.enqueue(
                TransactionChangedEvent.updated(
                        beforeSnapshot,
                        afterSnapshot
                )
        );

        return transactionMapper.toResponse(savedTransaction);
    }

    @Transactional
    public void deleteTransaction(
            Long userId,
            Long transactionId
    ) {
        FinancialTransaction transaction =
                transactionRepository
                        .findOwnedTransactionForUpdate(
                                transactionId,
                                userId
                        )
                        .orElseThrow(
                                () -> new FinancialTransactionNotFoundException(
                                        transactionId
                                )
                        );

        Account account = accountRepository
                .findOwnedAccountForUpdate(
                        transaction.getAccount().getId(),
                        userId,
                        AccountStatus.ACTIVE
                )
                .orElseThrow(
                        () -> new AccountNotFoundException(
                                transaction.getAccount().getId()
                        )
                );

        reverseBalanceEffect(
                account,
                transaction.getType(),
                transaction.getAmount()
        );
        TransactionSnapshot beforeSnapshot =
                toSnapshot(transaction);

        accountRepository.save(account);
        transactionRepository.delete(transaction);


        outboxEventService.enqueue(
                TransactionChangedEvent.deleted(
                        beforeSnapshot
                )
        );
    }

    private record AccountPair(
            Account oldAccount,
            Account newAccount
    ) {
    }

    private void validateCategoryType(
            Category category,
            TransactionType transactionType
    ) {
        if (category.getType() != transactionType) {
            throw new CategoryTypeMismatchException();
        }
    }


    private Category findOwnedCategory(
            Long userId,
            Long categoryId
    ) {
        return categoryRepository
                .findByIdAndUserId(categoryId, userId)
                .orElseThrow(
                        () -> new CategoryNotFoundException(categoryId)
                );
    }
    private AccountPair lockAccounts(
            Long userId,
            Long oldAccountId,
            Long newAccountId
    ) {
        if (oldAccountId.equals(newAccountId)) {
            Account account = lockAccount(
                    userId,
                    oldAccountId
            );

            return new AccountPair(account, account);
        }

        Long firstAccountId =
                Math.min(oldAccountId, newAccountId);

        Long secondAccountId =
                Math.max(oldAccountId, newAccountId);

        Account firstAccount =
                lockAccount(userId, firstAccountId);

        Account secondAccount =
                lockAccount(userId, secondAccountId);

        Account oldAccount =
                oldAccountId.equals(firstAccountId)
                        ? firstAccount
                        : secondAccount;

        Account newAccount =
                newAccountId.equals(firstAccountId)
                        ? firstAccount
                        : secondAccount;

        return new AccountPair(oldAccount, newAccount);
    }

    private Account lockAccount(
            Long userId,
            Long accountId
    ) {
        return accountRepository
                .findOwnedAccountForUpdate(
                        accountId,
                        userId,
                        AccountStatus.ACTIVE)
                .orElseThrow(
                        () -> new AccountNotFoundException(accountId)
                );
    }

    private void applyBalanceEffect(
            Account account,
            TransactionType type,
            BigDecimal amount
    ) {
        BigDecimal newBalance = switch (type) {
            case INCOME ->
                    account.getBalance().add(amount);

            case EXPENSE ->
                    account.getBalance().subtract(amount);
        };

        account.setBalance(newBalance);
    }
    private void reverseBalanceEffect(
            Account account,
            TransactionType type,
            BigDecimal amount
    ) {
        BigDecimal newBalance = switch (type) {
            case INCOME ->
                    account.getBalance().subtract(amount);

            case EXPENSE ->
                    account.getBalance().add(amount);
        };

        account.setBalance(newBalance);
    }

    private TransactionSnapshot toSnapshot(
            FinancialTransaction transaction
    ) {
        return new TransactionSnapshot(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getAccount().getId(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getType().name(),
                transaction.getAmount(),
                transaction.getTransactionDate()
        );
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();

        return normalized.isEmpty() ? null : normalized;
    }
}