package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.entity.RecurringExecutionStatus;
import com.smartwallet.financeservice.entity.RecurringTransaction;
import com.smartwallet.financeservice.entity.RecurringTransactionExecution;
import com.smartwallet.financeservice.repository.RecurringTransactionExecutionRepository;
import com.smartwallet.financeservice.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionExecutionHistoryService {
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final RecurringTransactionExecutionRepository recurringTransactionExecutionRepository;

    private final RecurringTransactionRepository recurringTransactionRepository;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void recordFailure(
            Long reccuringTransactionId,
            LocalDate scheduledDate,
            Throwable throwable
    ){
        RecurringTransaction recurringTransaction =
                recurringTransactionRepository.findByIdForUpdate(
                        reccuringTransactionId
                ).orElse(null);

        if (recurringTransaction == null) {
            log.warn("Recurring transaction not found for id {}", reccuringTransactionId);
            return;
        }

        RecurringTransactionExecution recurringTransactionExecution =
                recurringTransactionExecutionRepository.findPeriodForUpdate(
                        reccuringTransactionId,
                        scheduledDate
                ).orElseGet(
                        () -> RecurringTransactionExecution.builder()
                                .recurringTransaction(
                                        recurringTransaction
                                )
                                .scheduledDate(scheduledDate)
                                .createdAt(
                                        Instant.now()
                                )
                                .build()
                );
        if(recurringTransactionExecution.getStatus() == RecurringExecutionStatus.SUCCESS) {
            return;
        }

        recurringTransactionExecution.setStatus(
                RecurringExecutionStatus.FAILED
        );

        recurringTransactionExecution.setGeneratedTransactionId(null);

        recurringTransactionExecution.setErrorMessage(
                normalizedErrorMessage(throwable)
        );

        recurringTransactionExecution.setCompletedAt(Instant.now());

        recurringTransactionExecutionRepository.save(recurringTransactionExecution);
    }

    private String normalizedErrorMessage(Throwable throwable){
        Throwable source = throwable != null ? throwable : new RuntimeException("Unknown execution error");
        String message =
                source.getMessage();

        if (message == null
                || message.isBlank()) {
            message =
                    source.getClass()
                            .getSimpleName();
        }

        String normalized =
                message.trim();

        return normalized.length()
                <= MAX_ERROR_MESSAGE_LENGTH

                ? normalized

                : normalized.substring(
                0,
                MAX_ERROR_MESSAGE_LENGTH
        );
    }


}
