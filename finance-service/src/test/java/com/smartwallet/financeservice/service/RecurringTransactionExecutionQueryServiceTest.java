package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.response.RecurringTransactionExecutionResponse;
import com.smartwallet.financeservice.entity.RecurringExecutionStatus;
import com.smartwallet.financeservice.entity.RecurringTransaction;
import com.smartwallet.financeservice.entity.RecurringTransactionExecution;
import com.smartwallet.financeservice.exception.RecurringTransactionNotFoundException;
import com.smartwallet.financeservice.mapper.RecurringTransactionExecutionMapper;
import com.smartwallet.financeservice.repository.RecurringTransactionExecutionRepository;
import com.smartwallet.financeservice.repository.RecurringTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionExecutionQueryServiceTest {

    @Mock
    private RecurringTransactionRepository
            recurringTransactionRepository;

    @Mock
    private RecurringTransactionExecutionRepository
            executionRepository;

    @Mock
    private RecurringTransactionExecutionMapper
            executionMapper;

    @InjectMocks
    private RecurringTransactionExecutionQueryService
            executionQueryService;

    @Test
    void shouldReturnExecutionHistoryForOwnedRecurringTransaction() {
        Long userId = 10L;
        Long recurringTransactionId = 5L;

        RecurringTransaction recurringTransaction =
                RecurringTransaction.builder()
                        .id(recurringTransactionId)
                        .userId(userId)
                        .build();

        RecurringTransactionExecution newestExecution =
                RecurringTransactionExecution.builder()
                        .id(2L)
                        .recurringTransaction(
                                recurringTransaction
                        )
                        .scheduledDate(
                                LocalDate.of(
                                        2026,
                                        9,
                                        10
                                )
                        )
                        .status(
                                RecurringExecutionStatus.FAILED
                        )
                        .errorMessage(
                                "Account was not found"
                        )
                        .createdAt(
                                Instant.parse(
                                        "2026-09-10T00:00:00Z"
                                )
                        )
                        .completedAt(
                                Instant.parse(
                                        "2026-09-10T00:00:01Z"
                                )
                        )
                        .build();

        RecurringTransactionExecution olderExecution =
                RecurringTransactionExecution.builder()
                        .id(1L)
                        .recurringTransaction(
                                recurringTransaction
                        )
                        .scheduledDate(
                                LocalDate.of(
                                        2026,
                                        8,
                                        10
                                )
                        )
                        .status(
                                RecurringExecutionStatus.SUCCESS
                        )
                        .generatedTransactionId(42L)
                        .createdAt(
                                Instant.parse(
                                        "2026-08-10T00:00:00Z"
                                )
                        )
                        .completedAt(
                                Instant.parse(
                                        "2026-08-10T00:00:01Z"
                                )
                        )
                        .build();

        RecurringTransactionExecutionResponse newestResponse =
                new RecurringTransactionExecutionResponse(
                        2L,
                        LocalDate.of(
                                2026,
                                9,
                                10
                        ),
                        RecurringExecutionStatus.FAILED,
                        null,
                        "Account was not found",
                        2,
                        Instant.parse("2026-09-10T00:05:00Z"),
                        Instant.parse(
                                "2026-09-10T00:00:00Z"
                        ),
                        Instant.parse(
                                "2026-09-10T00:00:01Z"
                        )
                );

        RecurringTransactionExecutionResponse olderResponse =
                new RecurringTransactionExecutionResponse(
                        1L,
                        LocalDate.of(
                                2026,
                                8,
                                10
                        ),
                        RecurringExecutionStatus.SUCCESS,
                        42L,
                        null,
                        1,
                        null,
                        Instant.parse(
                                "2026-08-10T00:00:00Z"
                        ),
                        Instant.parse(
                                "2026-08-10T00:00:01Z"
                        )
                );

        when(
                recurringTransactionRepository
                        .findByIdAndUserId(
                                recurringTransactionId,
                                userId
                        )
        ).thenReturn(
                Optional.of(recurringTransaction)
        );

        when(
                executionRepository
                        .findAllByRecurringTransactionIdOrderByScheduledDateDesc(
                                recurringTransactionId
                        )
        ).thenReturn(
                List.of(
                        newestExecution,
                        olderExecution
                )
        );

        when(
                executionMapper.toResponse(
                        newestExecution
                )
        ).thenReturn(newestResponse);

        when(
                executionMapper.toResponse(
                        olderExecution
                )
        ).thenReturn(olderResponse);

        List<RecurringTransactionExecutionResponse> result =
                executionQueryService.getExecutionHistory(
                        userId,
                        recurringTransactionId
                );

        assertThat(result)
                .containsExactly(
                        newestResponse,
                        olderResponse
                );

        verify(recurringTransactionRepository)
                .findByIdAndUserId(
                        recurringTransactionId,
                        userId
                );

        verify(executionRepository)
                .findAllByRecurringTransactionIdOrderByScheduledDateDesc(
                        recurringTransactionId
                );
    }

    @Test
    void shouldReturnEmptyListWhenPlanHasNoExecutionHistory() {
        Long userId = 10L;
        Long recurringTransactionId = 5L;

        RecurringTransaction recurringTransaction =
                RecurringTransaction.builder()
                        .id(recurringTransactionId)
                        .userId(userId)
                        .build();

        when(
                recurringTransactionRepository
                        .findByIdAndUserId(
                                recurringTransactionId,
                                userId
                        )
        ).thenReturn(
                Optional.of(recurringTransaction)
        );

        when(
                executionRepository
                        .findAllByRecurringTransactionIdOrderByScheduledDateDesc(
                                recurringTransactionId
                        )
        ).thenReturn(List.of());

        List<RecurringTransactionExecutionResponse> result =
                executionQueryService.getExecutionHistory(
                        userId,
                        recurringTransactionId
                );

        assertThat(result)
                .isEmpty();

        verify(executionRepository)
                .findAllByRecurringTransactionIdOrderByScheduledDateDesc(
                        recurringTransactionId
                );
    }

    @Test
    void shouldRejectExecutionHistoryAccessForUnownedPlan() {
        Long userId = 20L;
        Long recurringTransactionId = 5L;

        when(
                recurringTransactionRepository
                        .findByIdAndUserId(
                                recurringTransactionId,
                                userId
                        )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> executionQueryService
                        .getExecutionHistory(
                                userId,
                                recurringTransactionId
                        )
        )
                .isInstanceOf(
                        RecurringTransactionNotFoundException.class
                );

        verify(recurringTransactionRepository)
                .findByIdAndUserId(
                        recurringTransactionId,
                        userId
                );

        verifyNoInteractions(
                executionRepository,
                executionMapper
        );
    }
}