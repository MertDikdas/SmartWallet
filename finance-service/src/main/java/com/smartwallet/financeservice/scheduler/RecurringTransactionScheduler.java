package com.smartwallet.financeservice.scheduler;

import com.smartwallet.financeservice.entity.RecurringTransactionStatus;
import com.smartwallet.financeservice.exception.RecurringTransactionExecutionException;
import com.smartwallet.financeservice.repository.RecurringTransactionRepository;
import com.smartwallet.financeservice.service.RecurringTransactionExecutionHistoryService;
import com.smartwallet.financeservice.service.RecurringTransactionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionScheduler {

    private static final int BATCH_SIZE = 50;

    private final RecurringTransactionRepository
            recurringTransactionRepository;

    private final RecurringTransactionExecutor
            recurringTransactionExecutor;

    private final RecurringTransactionExecutionHistoryService
            executionHistoryService;

    @Scheduled(
            fixedDelayString =
                    "${app.recurring-transactions.scheduler-delay-ms:60000}"
    )
    public void executeDueRecurringTransactions() {
        LocalDate today =
                LocalDate.now(ZoneOffset.UTC);

        List<Long> dueRecurringTransactionIds =
                recurringTransactionRepository
                        .findDueRecurringTransactionIds(
                                RecurringTransactionStatus.ACTIVE,
                                today,
                                PageRequest.of(
                                        0,
                                        BATCH_SIZE
                                )
                        );

        if (dueRecurringTransactionIds.isEmpty()) {
            return;
        }

        log.info(
                "Found {} due recurring transactions",
                dueRecurringTransactionIds.size()
        );

        for (Long recurringTransactionId
                : dueRecurringTransactionIds) {

            try {
                recurringTransactionExecutor.execute(
                        recurringTransactionId,
                        today
                );

            } catch (
                    RecurringTransactionExecutionException exception
            ) {
                Throwable originalCause =
                        exception.getCause() != null
                                ? exception.getCause()
                                : exception;

                try {
                    executionHistoryService.recordFailure(
                            exception.getRecurringTransactionId(),
                            exception.getScheduledDate(),
                            originalCause
                    );

                } catch (Exception historyException) {
                    log.error(
                            "Recurring transaction failure history could not be saved: recurringId={}, scheduledDate={}",
                            exception.getRecurringTransactionId(),
                            exception.getScheduledDate(),
                            historyException
                    );
                }

                log.error(
                        "Recurring transaction execution failed: recurringId={}, scheduledDate={}",
                        exception.getRecurringTransactionId(),
                        exception.getScheduledDate(),
                        originalCause
                );

            } catch (Exception exception) {
                log.error(
                        "Unexpected recurring transaction scheduler error: recurringId={}",
                        recurringTransactionId,
                        exception
                );
            }
        }
    }
}