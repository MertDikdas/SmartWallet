package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.CreateTransactionRequest;
import com.smartwallet.financeservice.dto.response.TransactionResponse;
import com.smartwallet.financeservice.entity.Account;
import com.smartwallet.financeservice.entity.Category;
import com.smartwallet.financeservice.entity.RecurrenceFrequency;
import com.smartwallet.financeservice.entity.RecurringExecutionStatus;
import com.smartwallet.financeservice.entity.RecurringTransaction;
import com.smartwallet.financeservice.entity.RecurringTransactionExecution;
import com.smartwallet.financeservice.entity.RecurringTransactionStatus;
import com.smartwallet.financeservice.entity.TransactionType;
import com.smartwallet.financeservice.exception.RecurringTransactionExecutionException;
import com.smartwallet.financeservice.repository.RecurringTransactionExecutionRepository;
import com.smartwallet.financeservice.repository.RecurringTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionExecutorTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private RecurringTransactionExecutionRepository executionRepository;

    @InjectMocks
    private RecurringTransactionExecutor recurringTransactionExecutor;

    @Test
    void shouldExecuteDueRecurringTransaction() {
        Long recurringTransactionId = 5L;
        Long userId = 10L;
        LocalDate executionDate = LocalDate.of(2026, 8, 10);

        Account account = Account.builder()
                .id(1L)
                .build();

        Category category = Category.builder()
                .id(2L)
                .build();

        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(recurringTransactionId)
                .userId(userId)
                .account(account)
                .category(category)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("8000.00"))
                .description("Monthly rent")
                .frequency(RecurrenceFrequency.MONTHLY)
                .status(RecurringTransactionStatus.ACTIVE)
                .startDate(executionDate)
                .nextExecutionDate(executionDate)
                .build();

        TransactionResponse transactionResponse = mock(TransactionResponse.class);

        when(recurringTransactionRepository.findByIdForUpdate(recurringTransactionId))
                .thenReturn(Optional.of(recurringTransaction));

        when(executionRepository.findPeriodForUpdate(
                recurringTransactionId,
                executionDate
        )).thenReturn(Optional.empty());

        when(executionRepository.saveAndFlush(
                any(RecurringTransactionExecution.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionResponse.id()).thenReturn(50L);

        when(transactionService.createTransaction(
                eq(userId),
                any(CreateTransactionRequest.class)
        )).thenReturn(transactionResponse);

        recurringTransactionExecutor.execute(
                recurringTransactionId,
                executionDate
        );

        ArgumentCaptor<CreateTransactionRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTransactionRequest.class);

        verify(transactionService).createTransaction(
                eq(userId),
                requestCaptor.capture()
        );

        CreateTransactionRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedRequest.accountId())
                .isEqualTo(account.getId());

        assertThat(capturedRequest.categoryId())
                .isEqualTo(category.getId());

        assertThat(capturedRequest.type())
                .isEqualTo(TransactionType.EXPENSE);

        assertThat(capturedRequest.amount())
                .isEqualByComparingTo("8000.00");

        assertThat(capturedRequest.description())
                .isEqualTo("Monthly rent");

        assertThat(capturedRequest.transactionDate())
                .isEqualTo(
                        executionDate
                                .atStartOfDay()
                                .toInstant(ZoneOffset.UTC)
                );

        assertThat(recurringTransaction.getLastExecutionDate())
                .isEqualTo(executionDate);

        assertThat(recurringTransaction.getNextExecutionDate())
                .isEqualTo(executionDate.plusMonths(1));

        assertThat(recurringTransaction.getStatus())
                .isEqualTo(RecurringTransactionStatus.ACTIVE);

        ArgumentCaptor<RecurringTransactionExecution> executionCaptor =
                ArgumentCaptor.forClass(RecurringTransactionExecution.class);

        verify(executionRepository).save(executionCaptor.capture());

        RecurringTransactionExecution savedExecution = executionCaptor.getValue();

        assertThat(savedExecution.getRecurringTransaction())
                .isEqualTo(recurringTransaction);

        assertThat(savedExecution.getScheduledDate())
                .isEqualTo(executionDate);

        assertThat(savedExecution.getStatus())
                .isEqualTo(RecurringExecutionStatus.SUCCESS);

        assertThat(savedExecution.getGeneratedTransactionId())
                .isEqualTo(50L);

        assertThat(savedExecution.getErrorMessage())
                .isNull();

        assertThat(savedExecution.getCompletedAt())
                .isNotNull();

        verify(recurringTransactionRepository)
                .save(recurringTransaction);
    }

    @Test
    void shouldNotExecuteWhenNextExecutionDateIsInFuture() {
        Long recurringTransactionId = 5L;
        LocalDate executionDate = LocalDate.of(2026, 8, 10);

        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(recurringTransactionId)
                .userId(10L)
                .status(RecurringTransactionStatus.ACTIVE)
                .nextExecutionDate(executionDate.plusDays(1))
                .build();

        when(recurringTransactionRepository.findByIdForUpdate(recurringTransactionId))
                .thenReturn(Optional.of(recurringTransaction));

        recurringTransactionExecutor.execute(
                recurringTransactionId,
                executionDate
        );

        verifyNoInteractions(transactionService);
        verifyNoInteractions(executionRepository);

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }

    @Test
    void shouldNotExecutePausedRecurringTransaction() {
        Long recurringTransactionId = 5L;
        LocalDate executionDate = LocalDate.of(2026, 8, 10);

        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(recurringTransactionId)
                .userId(10L)
                .status(RecurringTransactionStatus.PAUSED)
                .nextExecutionDate(executionDate)
                .build();

        when(recurringTransactionRepository.findByIdForUpdate(recurringTransactionId))
                .thenReturn(Optional.of(recurringTransaction));

        recurringTransactionExecutor.execute(
                recurringTransactionId,
                executionDate
        );

        verifyNoInteractions(transactionService);
        verifyNoInteractions(executionRepository);

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }

    @Test
    void shouldCancelPlanWhenEndDateHasPassed() {
        Long recurringTransactionId = 5L;
        LocalDate executionDate = LocalDate.of(2026, 8, 10);

        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(recurringTransactionId)
                .userId(10L)
                .status(RecurringTransactionStatus.ACTIVE)
                .nextExecutionDate(executionDate)
                .endDate(executionDate.minusDays(1))
                .build();

        when(recurringTransactionRepository.findByIdForUpdate(recurringTransactionId))
                .thenReturn(Optional.of(recurringTransaction));

        recurringTransactionExecutor.execute(
                recurringTransactionId,
                executionDate
        );

        assertThat(recurringTransaction.getStatus())
                .isEqualTo(RecurringTransactionStatus.CANCELLED);

        verifyNoInteractions(transactionService);
        verifyNoInteractions(executionRepository);

        verify(recurringTransactionRepository)
                .save(recurringTransaction);
    }

    @Test
    void shouldCancelPlanAfterFinalExecution() {
        Long recurringTransactionId = 5L;
        Long userId = 10L;
        LocalDate executionDate = LocalDate.of(2026, 8, 10);

        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(recurringTransactionId)
                .userId(userId)
                .account(Account.builder().id(1L).build())
                .category(Category.builder().id(2L).build())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .frequency(RecurrenceFrequency.WEEKLY)
                .status(RecurringTransactionStatus.ACTIVE)
                .startDate(executionDate)
                .endDate(executionDate)
                .nextExecutionDate(executionDate)
                .build();

        TransactionResponse transactionResponse = mock(TransactionResponse.class);

        when(recurringTransactionRepository.findByIdForUpdate(recurringTransactionId))
                .thenReturn(Optional.of(recurringTransaction));

        when(executionRepository.findPeriodForUpdate(
                recurringTransactionId,
                executionDate
        )).thenReturn(Optional.empty());

        when(executionRepository.saveAndFlush(
                any(RecurringTransactionExecution.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionResponse.id()).thenReturn(51L);

        when(transactionService.createTransaction(
                eq(userId),
                any(CreateTransactionRequest.class)
        )).thenReturn(transactionResponse);

        recurringTransactionExecutor.execute(
                recurringTransactionId,
                executionDate
        );

        verify(transactionService).createTransaction(
                eq(userId),
                any(CreateTransactionRequest.class)
        );

        assertThat(recurringTransaction.getLastExecutionDate())
                .isEqualTo(executionDate);

        assertThat(recurringTransaction.getNextExecutionDate())
                .isEqualTo(executionDate.plusWeeks(1));

        assertThat(recurringTransaction.getStatus())
                .isEqualTo(RecurringTransactionStatus.CANCELLED);

        ArgumentCaptor<RecurringTransactionExecution> executionCaptor =
                ArgumentCaptor.forClass(RecurringTransactionExecution.class);

        verify(executionRepository).save(executionCaptor.capture());

        assertThat(executionCaptor.getValue().getStatus())
                .isEqualTo(RecurringExecutionStatus.SUCCESS);

        assertThat(executionCaptor.getValue().getGeneratedTransactionId())
                .isEqualTo(51L);

        verify(recurringTransactionRepository)
                .save(recurringTransaction);
    }

    @Test
    void shouldNotCreateDuplicateTransactionWhenExecutionAlreadySucceeded() {
        Long recurringTransactionId = 5L;
        Long userId = 10L;
        LocalDate executionDate = LocalDate.of(2026, 8, 10);

        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(recurringTransactionId)
                .userId(userId)
                .frequency(RecurrenceFrequency.MONTHLY)
                .status(RecurringTransactionStatus.ACTIVE)
                .nextExecutionDate(executionDate)
                .build();

        RecurringTransactionExecution successfulExecution =
                RecurringTransactionExecution.builder()
                        .recurringTransaction(recurringTransaction)
                        .scheduledDate(executionDate)
                        .status(RecurringExecutionStatus.SUCCESS)
                        .generatedTransactionId(50L)
                        .build();

        when(recurringTransactionRepository.findByIdForUpdate(recurringTransactionId))
                .thenReturn(Optional.of(recurringTransaction));

        when(executionRepository.findPeriodForUpdate(
                recurringTransactionId,
                executionDate
        )).thenReturn(Optional.of(successfulExecution));

        recurringTransactionExecutor.execute(
                recurringTransactionId,
                executionDate
        );

        verifyNoInteractions(transactionService);

        assertThat(recurringTransaction.getLastExecutionDate())
                .isEqualTo(executionDate);

        assertThat(recurringTransaction.getNextExecutionDate())
                .isEqualTo(executionDate.plusMonths(1));

        verify(executionRepository, never())
                .save(any(RecurringTransactionExecution.class));

        verify(recurringTransactionRepository)
                .save(recurringTransaction);
    }

    @Test
    void shouldNotAdvancePlanWhenTransactionCreationFails() {
        Long recurringTransactionId = 5L;
        Long userId = 10L;
        LocalDate executionDate = LocalDate.of(2026, 8, 10);

        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(recurringTransactionId)
                .userId(userId)
                .account(Account.builder().id(1L).build())
                .category(Category.builder().id(2L).build())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("500.00"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .status(RecurringTransactionStatus.ACTIVE)
                .startDate(executionDate)
                .nextExecutionDate(executionDate)
                .build();

        when(recurringTransactionRepository.findByIdForUpdate(recurringTransactionId))
                .thenReturn(Optional.of(recurringTransaction));

        when(executionRepository.findPeriodForUpdate(
                recurringTransactionId,
                executionDate
        )).thenReturn(Optional.empty());

        when(executionRepository.saveAndFlush(
                any(RecurringTransactionExecution.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        RuntimeException transactionFailure =
                new RuntimeException("Transaction creation failed");

        org.mockito.Mockito.doThrow(transactionFailure)
                .when(transactionService)
                .createTransaction(
                        eq(userId),
                        any(CreateTransactionRequest.class)
                );

        assertThatThrownBy(
                () -> recurringTransactionExecutor.execute(
                        recurringTransactionId,
                        executionDate
                )
        )
                .isInstanceOf(RecurringTransactionExecutionException.class)
                .hasCause(transactionFailure);

        assertThat(recurringTransaction.getLastExecutionDate())
                .isNull();

        assertThat(recurringTransaction.getNextExecutionDate())
                .isEqualTo(executionDate);

        assertThat(recurringTransaction.getStatus())
                .isEqualTo(RecurringTransactionStatus.ACTIVE);

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));

        verify(executionRepository, never())
                .save(any(RecurringTransactionExecution.class));
    }
}