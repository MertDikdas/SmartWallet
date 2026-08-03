package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.CreateTransactionRequest;
import com.smartwallet.financeservice.entity.RecurrenceFrequency;
import com.smartwallet.financeservice.entity.RecurringTransaction;
import com.smartwallet.financeservice.entity.RecurringTransactionStatus;
import com.smartwallet.financeservice.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionExecutor {

    private final RecurringTransactionRepository
            recurringTransactionRepository;

    private final TransactionService transactionService;

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

                        recurringTransaction.getDescription(),

                        scheduledDate
                                .atStartOfDay()
                                .toInstant(ZoneOffset.UTC)
                );


        transactionService.createTransaction(
                recurringTransaction.getUserId(),
                request
        );

        recurringTransaction.setLastExecutionDate(
                scheduledDate
        );

        LocalDate nextExecutionDate =
                calculateNextExecutionDate(
                        scheduledDate,
                        recurringTransaction.getFrequency()
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

        recurringTransactionRepository.save(
                recurringTransaction
        );

        log.info(
                "Recurring transaction executed: recurringId={}, scheduledDate={}, nextExecutionDate={}",
                recurringTransactionId,
                scheduledDate,
                nextExecutionDate
        );
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