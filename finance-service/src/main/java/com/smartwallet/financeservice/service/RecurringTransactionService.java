package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.CreateRecurringTransactionRequest;
import com.smartwallet.financeservice.dto.response.RecurringTransactionResponse;
import com.smartwallet.financeservice.entity.Account;
import com.smartwallet.financeservice.entity.AccountStatus;
import com.smartwallet.financeservice.entity.Category;
import com.smartwallet.financeservice.entity.RecurrenceFrequency;
import com.smartwallet.financeservice.entity.RecurringTransaction;
import com.smartwallet.financeservice.entity.RecurringTransactionStatus;
import com.smartwallet.financeservice.exception.AccountNotFoundException;
import com.smartwallet.financeservice.exception.CategoryNotFoundException;
import com.smartwallet.financeservice.exception.InvalidRecurringTransactionStateException;
import com.smartwallet.financeservice.exception.RecurringTransactionCategoryMismatchException;
import com.smartwallet.financeservice.exception.RecurringTransactionNotFoundException;
import com.smartwallet.financeservice.mapper.RecurringTransactionMapper;
import com.smartwallet.financeservice.repository.AccountRepository;
import com.smartwallet.financeservice.repository.CategoryRepository;
import com.smartwallet.financeservice.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository
            recurringTransactionRepository;

    private final AccountRepository accountRepository;

    private final CategoryRepository categoryRepository;

    private final RecurringTransactionMapper
            recurringTransactionMapper;

    @Transactional
    public RecurringTransactionResponse createRecurringTransaction(
            Long userId,
            CreateRecurringTransactionRequest request
    ) {
        Account account =
                accountRepository
                        .findByIdAndUserIdAndStatus(
                                request.accountId(),
                                userId,
                                AccountStatus.ACTIVE
                        )
                        .orElseThrow(
                                ()-> new AccountNotFoundException(request.accountId())
                        );

        Category category =
                categoryRepository
                        .findByIdAndUserId(
                                request.categoryId(),
                                userId
                        )
                        .orElseThrow(
                                ()-> new CategoryNotFoundException(request.categoryId())
                        );

        validateCategoryType(
                category,
                request
        );

        RecurringTransaction recurringTransaction =
                RecurringTransaction.builder()
                        .userId(userId)
                        .account(account)
                        .category(category)
                        .type(request.type())
                        .amount(request.amount())
                        .description(
                                normalizeDescription(
                                        request.description()
                                )
                        )
                        .frequency(request.frequency())
                        .status(
                                RecurringTransactionStatus.ACTIVE
                        )
                        .startDate(request.startDate())
                        .endDate(request.endDate())
                        .nextExecutionDate(
                                request.startDate()
                        )
                        .build();

        RecurringTransaction savedRecurringTransaction =
                recurringTransactionRepository.save(
                        recurringTransaction
                );

        return recurringTransactionMapper.toResponse(
                savedRecurringTransaction
        );
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse>
    getRecurringTransactions(
            Long userId
    ) {
        return recurringTransactionRepository
                .findAllByUserIdOrderByCreatedAtDesc(
                        userId
                )
                .stream()
                .map(
                        recurringTransactionMapper::toResponse
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponse
    getRecurringTransaction(
            Long userId,
            Long recurringTransactionId
    ) {
        RecurringTransaction recurringTransaction =
                getOwnedRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        return recurringTransactionMapper.toResponse(
                recurringTransaction
        );
    }

    @Transactional
    public RecurringTransactionResponse pauseRecurringTransaction(
            Long userId,
            Long recurringTransactionId
    ) {
        RecurringTransaction recurringTransaction =
                getOwnedRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        if (recurringTransaction.getStatus()
                == RecurringTransactionStatus.CANCELLED) {
            throw new InvalidRecurringTransactionStateException(
                    "Cancelled recurring transaction cannot be paused"
            );
        }

        if (recurringTransaction.getStatus()
                == RecurringTransactionStatus.PAUSED) {
            return recurringTransactionMapper.toResponse(
                    recurringTransaction
            );
        }

        recurringTransaction.setStatus(
                RecurringTransactionStatus.PAUSED
        );

        RecurringTransaction savedRecurringTransaction =
                recurringTransactionRepository.save(
                        recurringTransaction
                );

        return recurringTransactionMapper.toResponse(
                savedRecurringTransaction
        );
    }

    @Transactional
    public RecurringTransactionResponse resumeRecurringTransaction(
            Long userId,
            Long recurringTransactionId
    ) {
        RecurringTransaction recurringTransaction =
                getOwnedRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        if (recurringTransaction.getStatus()
                == RecurringTransactionStatus.CANCELLED) {
            throw new InvalidRecurringTransactionStateException(
                    "Cancelled recurring transaction cannot be resumed"
            );
        }

        if (recurringTransaction.getStatus()
                == RecurringTransactionStatus.ACTIVE) {
            return recurringTransactionMapper.toResponse(
                    recurringTransaction
            );
        }

        LocalDate nextExecutionDate =
                moveExecutionDateToPresentOrFuture(
                        recurringTransaction
                );

        if (recurringTransaction.getEndDate() != null
                && nextExecutionDate.isAfter(
                recurringTransaction.getEndDate()
        )) {
            throw new InvalidRecurringTransactionStateException(
                    "Recurring transaction end date has already passed"
            );
        }

        recurringTransaction.setNextExecutionDate(
                nextExecutionDate
        );

        recurringTransaction.setStatus(
                RecurringTransactionStatus.ACTIVE
        );

        RecurringTransaction savedRecurringTransaction =
                recurringTransactionRepository.save(
                        recurringTransaction
                );

        return recurringTransactionMapper.toResponse(
                savedRecurringTransaction
        );
    }

    @Transactional
    public void cancelRecurringTransaction(
            Long userId,
            Long recurringTransactionId
    ) {
        RecurringTransaction recurringTransaction =
                getOwnedRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        if (recurringTransaction.getStatus()
                == RecurringTransactionStatus.CANCELLED) {
            return;
        }

        recurringTransaction.setStatus(
                RecurringTransactionStatus.CANCELLED
        );

        recurringTransactionRepository.save(
                recurringTransaction
        );
    }

    private RecurringTransaction getOwnedRecurringTransaction(
            Long userId,
            Long recurringTransactionId
    ) {
        return recurringTransactionRepository
                .findByIdAndUserId(
                        recurringTransactionId,
                        userId
                )
                .orElseThrow(
                        RecurringTransactionNotFoundException::new
                );
    }

    private void validateCategoryType(
            Category category,
            CreateRecurringTransactionRequest request
    ) {
        if (!category
                .getType()
                .name()
                .equals(request.type().name())) {
            throw new RecurringTransactionCategoryMismatchException();
        }
    }

    private LocalDate moveExecutionDateToPresentOrFuture(
            RecurringTransaction recurringTransaction
    ) {
        LocalDate executionDate =
                recurringTransaction.getNextExecutionDate();

        LocalDate today =
                LocalDate.now(ZoneOffset.UTC);

        while (executionDate.isBefore(today)) {
            executionDate =
                    calculateNextExecutionDate(
                            executionDate,
                            recurringTransaction.getFrequency()
                    );
        }

        return executionDate;
    }

    private LocalDate calculateNextExecutionDate(
            LocalDate currentExecutionDate,
            RecurrenceFrequency frequency
    ) {
        return switch (frequency) {
            case WEEKLY ->
                    currentExecutionDate.plusWeeks(1);

            case MONTHLY ->
                    currentExecutionDate.plusMonths(1);
        };
    }

    private String normalizeDescription(
            String description
    ) {
        if (description == null) {
            return null;
        }

        String normalized =
                description.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}