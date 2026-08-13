package com.smartwallet.financeservice.service;

import com.smartwallet.contracts.recurring.RecurringTransactionFailedEvent;
import com.smartwallet.financeservice.config.RecurringRetryProperties;
import com.smartwallet.financeservice.entity.RecurringExecutionStatus;
import com.smartwallet.financeservice.entity.RecurringTransaction;
import com.smartwallet.financeservice.entity.RecurringTransactionExecution;
import com.smartwallet.financeservice.entity.RecurringTransactionStatus;
import com.smartwallet.financeservice.outbox.OutboxEventService;
import com.smartwallet.financeservice.repository.RecurringTransactionExecutionRepository;
import com.smartwallet.financeservice.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionExecutionHistoryService {
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final RecurringTransactionExecutionRepository recurringTransactionExecutionRepository;

    private final RecurringTransactionRepository recurringTransactionRepository;

    private final RecurringRetryProperties retryProperties;

    private final OutboxEventService outboxEventService;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void recordFailure(
            Long recurringTransactionId,
            LocalDate scheduledDate,
            Throwable throwable
    ){
        RecurringTransaction recurringTransaction =
                recurringTransactionRepository.findByIdForUpdate(
                        recurringTransactionId
                ).orElse(null);

        if (recurringTransaction == null) {
            log.warn("Recurring transaction not found for id {}", recurringTransactionId);
            return;
        }

        RecurringTransactionExecution recurringTransactionExecution =
                recurringTransactionExecutionRepository.findPeriodForUpdate(
                        recurringTransactionId,
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
        Instant now = Instant.now();
        int nextAttemptCount =
                recurringTransactionExecution.getAttemptCount() == null
                        ? 1
                        : recurringTransactionExecution.getAttemptCount() + 1;

        recurringTransactionExecution.setAttemptCount(nextAttemptCount);

        recurringTransactionExecution.setStatus(
                RecurringExecutionStatus.FAILED
        );

        recurringTransactionExecution.setGeneratedTransactionId(null);

        recurringTransactionExecution.setErrorMessage(
                normalizedErrorMessage(throwable)
        );

        recurringTransactionExecution.setCompletedAt(now);

        if (nextAttemptCount < retryProperties.getMaxAttempts()) {
            Duration retryDelay =
                    nextAttemptCount == 1
                            ? retryProperties.getFirstDelay()
                            : retryProperties.getSecondDelay();

            recurringTransactionExecution.setNextRetryAt(
                    now.plus(retryDelay)
            );

        } else {
            recurringTransactionExecution.setNextRetryAt(null);

            recurringTransaction.setStatus(
                    RecurringTransactionStatus.PAUSED
            );

            recurringTransactionRepository.save(
                    recurringTransaction
            );
            RecurringTransactionFailedEvent event =
                    new RecurringTransactionFailedEvent(
                            UUID.randomUUID(),
                            now,
                            recurringTransaction.getId(),
                            recurringTransaction.getUserId(),
                            scheduledDate,
                            nextAttemptCount,
                            recurringTransactionExecution.getErrorMessage()
                    );

            outboxEventService.enqueue(event);

            log.warn(
                    "Recurring transaction paused after {} failed attempts: recurringId={}, scheduledDate={}",
                    nextAttemptCount,
                    recurringTransactionId,
                    scheduledDate
            );
        }

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
