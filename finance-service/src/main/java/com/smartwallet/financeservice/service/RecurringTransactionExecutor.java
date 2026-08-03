package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.CreateTransactionRequest;
import com.smartwallet.financeservice.dto.response.TransactionResponse;
import com.smartwallet.financeservice.entity.*;
import com.smartwallet.financeservice.exception.RecurringTransactionExecutionException;
import com.smartwallet.financeservice.repository.RecurringTransactionExecutionRepository;
import com.smartwallet.financeservice.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionExecutor {

    private final RecurringTransactionRepository
            recurringTransactionRepository;

    private final TransactionService transactionService;

    private final RecurringTransactionExecutionRepository recurringTransactionExecutionRepository;


    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void execute(
            Long recurringTransactionId,
            LocalDate executionDate
    ) {
        RecurringTransaction recurringTransaction =
                recurringTransactionRepository
                        .findByIdForUpdate(
                                recurringTransactionId
                        )
                        .orElse(null);

        if (recurringTransaction == null) {
            log.warn(
                    "Recurring transaction was not found: id={}",
                    recurringTransactionId
            );

            return;
        }

        if (recurringTransaction.getStatus()
                != RecurringTransactionStatus.ACTIVE) {
            return;
        }

        LocalDate scheduledDate =
                recurringTransaction
                        .getNextExecutionDate();

        if (scheduledDate == null
                || scheduledDate.isAfter(executionDate)) {
            return;
        }

        if (recurringTransaction.getEndDate() != null
                && scheduledDate.isAfter(
                recurringTransaction.getEndDate()
        )) {
            recurringTransaction.setStatus(
                    RecurringTransactionStatus.CANCELLED
            );

            recurringTransactionRepository.save(
                    recurringTransaction
            );

            return;
        }

        try {
            RecurringTransactionExecution execution =
                    prepareExecution(
                            recurringTransaction,
                            scheduledDate
                    );

            if (execution.getStatus()
                    == RecurringExecutionStatus.SUCCESS) {

                advanceRecurringTransaction(
                        recurringTransaction,
                        scheduledDate
                );

                recurringTransactionRepository.save(
                        recurringTransaction
                );

                return;
            }

            CreateTransactionRequest request =
                    new CreateTransactionRequest(
                            recurringTransaction
                                    .getAccount()
                                    .getId(),

                            recurringTransaction
                                    .getCategory()
                                    .getId(),

                            recurringTransaction.getType(),

                            recurringTransaction.getAmount(),

                            recurringTransaction
                                    .getDescription(),

                            scheduledDate
                                    .atStartOfDay()
                                    .toInstant(ZoneOffset.UTC)
                    );

            TransactionResponse transactionResponse =
                    transactionService.createTransaction(
                            recurringTransaction
                                    .getUserId(),
                            request
                    );

            execution.setStatus(
                    RecurringExecutionStatus.SUCCESS
            );

            execution.setGeneratedTransactionId(
                    transactionResponse.id()
            );

            execution.setErrorMessage(null);

            execution.setCompletedAt(
                    Instant.now()
            );

            advanceRecurringTransaction(
                    recurringTransaction,
                    scheduledDate
            );

            recurringTransactionExecutionRepository.save(execution);

            recurringTransactionRepository.save(
                    recurringTransaction
            );

            log.info(
                    "Recurring transaction executed: recurringId={}, scheduledDate={}, generatedTransactionId={}, nextExecutionDate={}",
                    recurringTransactionId,
                    scheduledDate,
                    transactionResponse.id(),
                    recurringTransaction.getNextExecutionDate()
            );

        } catch (RuntimeException exception) {

            throw new RecurringTransactionExecutionException(
                    recurringTransactionId,
                    scheduledDate,
                    exception
            );
        }
    }

    private RecurringTransactionExecution prepareExecution(
            RecurringTransaction recurringTransaction,
            LocalDate scheduledDate
    ) {
        RecurringTransactionExecution execution =
                recurringTransactionExecutionRepository
                        .findPeriodForUpdate(
                                recurringTransaction.getId(),
                                scheduledDate
                        )
                        .orElseGet(
                                () -> RecurringTransactionExecution
                                        .builder()
                                        .recurringTransaction(
                                                recurringTransaction
                                        )
                                        .scheduledDate(
                                                scheduledDate
                                        )
                                        .createdAt(
                                                Instant.now()
                                        )
                                        .build()
                        );

        if (execution.getStatus()
                == RecurringExecutionStatus.SUCCESS) {
            return execution;
        }

        execution.setStatus(
                RecurringExecutionStatus.PROCESSING
        );

        execution.setGeneratedTransactionId(null);

        execution.setErrorMessage(null);

        execution.setCompletedAt(null);

        return recurringTransactionExecutionRepository.saveAndFlush(
                execution
        );
    }

    private void advanceRecurringTransaction(
            RecurringTransaction recurringTransaction,
            LocalDate scheduledDate
    ) {
        recurringTransaction.setLastExecutionDate(
                scheduledDate
        );

        LocalDate nextExecutionDate =
                calculateNextExecutionDate(
                        scheduledDate,
                        recurringTransaction
                                .getFrequency()
                );

        recurringTransaction.setNextExecutionDate(
                nextExecutionDate
        );

        if (recurringTransaction.getEndDate() != null
                && nextExecutionDate.isAfter(
                recurringTransaction.getEndDate()
        )) {
            recurringTransaction.setStatus(
                    RecurringTransactionStatus.CANCELLED
            );
        }
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
}