package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.config.RecurringRetryProperties;
import com.smartwallet.financeservice.entity.RecurringExecutionStatus;
import com.smartwallet.financeservice.entity.RecurringTransaction;
import com.smartwallet.financeservice.entity.RecurringTransactionExecution;
import com.smartwallet.financeservice.entity.RecurringTransactionStatus;
import com.smartwallet.financeservice.repository.RecurringTransactionExecutionRepository;
import com.smartwallet.financeservice.repository.RecurringTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class RecurringTransactionExecutionHistoryServiceTest {

    @Mock
    private RecurringTransactionExecutionRepository recurringTransactionExecutionRepository;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Spy
    private RecurringRetryProperties retryProperties =
            new RecurringRetryProperties();

    @InjectMocks
    private RecurringTransactionExecutionHistoryService executionHistoryService;

    @Test
    public void shouldScheduleFirstRetryAfterOneMinute(){
        Long recurringTransactionId = 5L;

        LocalDate scheduledDate = LocalDate.of(2026,8,10);

        RecurringTransaction recurringTransaction =
                RecurringTransaction
                        .builder()
                        .id(recurringTransactionId)
                        .status(RecurringTransactionStatus.ACTIVE)
                        .build();

        RecurringTransactionExecution execution =
                RecurringTransactionExecution.builder()
                        .id(20L)
                        .recurringTransaction(
                                recurringTransaction
                        )
                        .scheduledDate(scheduledDate)
                        .status(
                                RecurringExecutionStatus.FAILED
                        )
                        .attemptCount(0)
                        .build();

        when(
                recurringTransactionRepository
                        .findByIdForUpdate(
                                recurringTransactionId
                        )
        ).thenReturn(
                Optional.of(recurringTransaction)
        );

        when(
                recurringTransactionExecutionRepository.findPeriodForUpdate(
                        recurringTransactionId,
                        scheduledDate
                )
        ).thenReturn(Optional.of(execution));

        RuntimeException failure =
                new RuntimeException(
                "Account was not found"
                );


        Instant beforeExecution = Instant.now();

        executionHistoryService.recordFailure(
                recurringTransactionId,
                scheduledDate,
                failure
        );

        Instant afterExecution = Instant.now();

        assertThat(execution.getAttemptCount()).isEqualTo(1);

        assertThat(execution.getStatus()).isEqualTo(RecurringExecutionStatus.FAILED);

        assertThat(execution.getGeneratedTransactionId()).isNull();
        assertThat(execution.getErrorMessage())
                .isEqualTo(
                        "Account was not found"
                );
        assertThat(execution.getCompletedAt())
                .isBetween(
                        beforeExecution,
                        afterExecution
                );

        assertThat(execution.getNextRetryAt())
                .isBetween(
                        beforeExecution.plusSeconds(60),
                        afterExecution.plusSeconds(60)
                );

        assertThat(recurringTransaction.getStatus())
                .isEqualTo(
                        RecurringTransactionStatus.ACTIVE
                );

        verify(recurringTransactionExecutionRepository)
                .save(execution);

        verify(
                recurringTransactionRepository,
                never()
        ).save(recurringTransaction);
    }

    @Test
    public void shouldScheduleSecondRetryAfterFiveMinutes(){
        Long recurringTransactionId = 5L;
        LocalDate scheduledDate = LocalDate.of(2026,8,10);

        RecurringTransaction recurringTransaction =
                RecurringTransaction.builder()
                        .id(recurringTransactionId)
                        .status(RecurringTransactionStatus.ACTIVE)
                        .build();

        RecurringTransactionExecution execution =
                RecurringTransactionExecution.builder()
                        .id(20L)
                        .recurringTransaction(
                                recurringTransaction
                        )
                        .scheduledDate(scheduledDate)
                        .status(
                                RecurringExecutionStatus.FAILED
                        )
                        .attemptCount(1)
                        .nextRetryAt(
                                Instant.parse(
                                        "2026-08-10T00:01:00Z"
                                )
                        )
                        .build();

        when(
                recurringTransactionRepository.findByIdForUpdate(
                        recurringTransactionId
                )
        ).thenReturn(Optional.of(recurringTransaction));

        when(
                recurringTransactionExecutionRepository.findPeriodForUpdate(
                        recurringTransactionId,
                        scheduledDate
                )
        ).thenReturn(Optional.of(execution));

        RuntimeException failure =
                new RuntimeException(
                        "Account was not found"
                );
        Instant beforeExecution = Instant.now();

        executionHistoryService.recordFailure(
                recurringTransactionId,
                scheduledDate,
                failure
        );

        Instant afterExecution = Instant.now();

        assertThat(execution.getAttemptCount()).isEqualTo(2);

        assertThat(execution.getStatus()).isEqualTo((RecurringExecutionStatus.FAILED));

        assertThat(execution.getGeneratedTransactionId()).isNull();

        assertThat(execution.getErrorMessage()).isEqualTo("Account was not found");

        assertThat(execution.getCompletedAt())
                .isBetween(
                        beforeExecution,
                        afterExecution
                );
        assertThat(execution.getNextRetryAt())
                .isBetween(
                        beforeExecution.plusSeconds(300),
                        afterExecution.plusSeconds(300)
                );

        assertThat(recurringTransaction.getStatus())
                .isEqualTo(
                        RecurringTransactionStatus.ACTIVE
                );

        verify(recurringTransactionExecutionRepository)
                .save(execution);

        verify(
                recurringTransactionRepository,
                never()
        ).save(recurringTransaction);

    }

    @Test
    void shouldPauseRecurringTransactionAfterThirdFailure() {
        Long recurringTransactionId = 5L;

        LocalDate scheduledDate =
                LocalDate.of(2026, 8, 10);

        RecurringTransaction recurringTransaction =
                RecurringTransaction.builder()
                        .id(recurringTransactionId)
                        .status(
                                RecurringTransactionStatus.ACTIVE
                        )
                        .build();

        RecurringTransactionExecution execution =
                RecurringTransactionExecution.builder()
                        .id(20L)
                        .recurringTransaction(
                                recurringTransaction
                        )
                        .scheduledDate(scheduledDate)
                        .status(
                                RecurringExecutionStatus.FAILED
                        )
                        .attemptCount(2)
                        .nextRetryAt(
                                Instant.parse(
                                        "2026-08-10T00:05:00Z"
                                )
                        )
                        .build();

        when(
                recurringTransactionRepository
                        .findByIdForUpdate(
                                recurringTransactionId
                        )
        ).thenReturn(
                Optional.of(recurringTransaction)
        );

        when(
                recurringTransactionExecutionRepository.findPeriodForUpdate(
                        recurringTransactionId,
                        scheduledDate
                )
        ).thenReturn(
                Optional.of(execution)
        );

        RuntimeException failure =
                new RuntimeException(
                        "Transaction creation failed"
                );

        Instant beforeExecution = Instant.now();

        executionHistoryService.recordFailure(
                recurringTransactionId,
                scheduledDate,
                failure
        );

        Instant afterExecution = Instant.now();

        assertThat(execution.getAttemptCount())
                .isEqualTo(3);

        assertThat(execution.getStatus())
                .isEqualTo(
                        RecurringExecutionStatus.FAILED
                );

        assertThat(execution.getErrorMessage())
                .isEqualTo(
                        "Transaction creation failed"
                );

        assertThat(execution.getCompletedAt())
                .isBetween(
                        beforeExecution,
                        afterExecution
                );

        assertThat(execution.getNextRetryAt())
                .isNull();

        assertThat(recurringTransaction.getStatus())
                .isEqualTo(
                        RecurringTransactionStatus.PAUSED
                );

        verify(recurringTransactionRepository)
                .save(recurringTransaction);

        verify(recurringTransactionExecutionRepository)
                .save(execution);
    }

    @Test
    void shouldNotOverwriteSuccessfulExecutionWithFailure() {
        Long recurringTransactionId = 5L;

        LocalDate scheduledDate =
                LocalDate.of(2026, 8, 10);

        RecurringTransaction recurringTransaction =
                RecurringTransaction.builder()
                        .id(recurringTransactionId)
                        .status(
                                RecurringTransactionStatus.ACTIVE
                        )
                        .build();

        RecurringTransactionExecution execution =
                RecurringTransactionExecution.builder()
                        .id(20L)
                        .recurringTransaction(
                                recurringTransaction
                        )
                        .scheduledDate(scheduledDate)
                        .status(
                                RecurringExecutionStatus.SUCCESS
                        )
                        .attemptCount(1)
                        .generatedTransactionId(42L)
                        .completedAt(
                                Instant.parse(
                                        "2026-08-10T00:00:05Z"
                                )
                        )
                        .build();

        when(
                recurringTransactionRepository
                        .findByIdForUpdate(
                                recurringTransactionId
                        )
        ).thenReturn(
                Optional.of(recurringTransaction)
        );

        when(
                recurringTransactionExecutionRepository.findPeriodForUpdate(
                        recurringTransactionId,
                        scheduledDate
                )
        ).thenReturn(
                Optional.of(execution)
        );

        executionHistoryService.recordFailure(
                recurringTransactionId,
                scheduledDate,
                new RuntimeException(
                        "Late scheduler error"
                )
        );

        assertThat(execution.getStatus())
                .isEqualTo(
                        RecurringExecutionStatus.SUCCESS
                );

        assertThat(execution.getAttemptCount())
                .isEqualTo(1);

        assertThat(execution.getGeneratedTransactionId())
                .isEqualTo(42L);

        assertThat(execution.getErrorMessage())
                .isNull();

        verify(
                recurringTransactionExecutionRepository,
                never()
        ).save(any(RecurringTransactionExecution.class));

        verify(
                recurringTransactionRepository,
                never()
        ).save(recurringTransaction);
    }

    @Test
    void shouldDoNothingWhenRecurringTransactionDoesNotExist() {
        Long recurringTransactionId = 999L;

        LocalDate scheduledDate =
                LocalDate.of(2026, 8, 10);

        when(
                recurringTransactionRepository
                        .findByIdForUpdate(
                                recurringTransactionId
                        )
        ).thenReturn(Optional.empty());

        executionHistoryService.recordFailure(
                recurringTransactionId,
                scheduledDate,
                new RuntimeException(
                        "Transaction failed"
                )
        );

        verifyNoInteractions(recurringTransactionExecutionRepository);

        verify(
                recurringTransactionRepository,
                never()
        ).save(any(RecurringTransaction.class));
    }


}
