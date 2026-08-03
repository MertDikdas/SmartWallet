package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.CreateRecurringTransactionRequest;
import com.smartwallet.financeservice.dto.response.RecurringTransactionResponse;
import com.smartwallet.financeservice.entity.*;
import com.smartwallet.financeservice.exception.RecurringTransactionCategoryMismatchException;
import com.smartwallet.financeservice.mapper.RecurringTransactionMapper;
import com.smartwallet.financeservice.repository.AccountRepository;
import com.smartwallet.financeservice.repository.CategoryRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository
            recurringTransactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RecurringTransactionMapper
            recurringTransactionMapper;

    @InjectMocks
    private RecurringTransactionService
            recurringTransactionService;

    @Test
    void shouldCreateRecurringTransaction() {
        Long userId = 10L;
        Long accountId = 1L;
        Long categoryId = 2L;

        LocalDate startDate =
                LocalDate.now(ZoneOffset.UTC)
                        .plusDays(1);

        Account account =
                Account.builder()
                        .id(accountId)
                        .userId(userId)
                        .name("Main Account")
                        .type(AccountType.CHECKING)
                        .currency(CurrencyCode.TRY)
                        .balance(new BigDecimal("1000.00"))
                        .status(AccountStatus.ACTIVE)
                        .build();

        Category category =
                Category.builder()
                        .id(categoryId)
                        .userId(userId)
                        .name("Rent")
                        .type(TransactionType.EXPENSE)
                        .build();

        CreateRecurringTransactionRequest request =
                new CreateRecurringTransactionRequest(
                        accountId,
                        categoryId,
                        TransactionType.EXPENSE,
                        new BigDecimal("8000.00"),
                        "  Monthly rent  ",
                        RecurrenceFrequency.MONTHLY,
                        startDate,
                        null
                );

        RecurringTransactionResponse expectedResponse =
                mock(RecurringTransactionResponse.class);

        when(
                accountRepository
                        .findByIdAndUserIdAndStatus(
                                accountId,
                                userId,
                                AccountStatus.ACTIVE
                        )
        ).thenReturn(Optional.of(account));

        when(
                categoryRepository.findByIdAndUserId(
                        categoryId,
                        userId
                )
        ).thenReturn(Optional.of(category));

        when(
                recurringTransactionRepository.save(
                        any(RecurringTransaction.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        when(
                recurringTransactionMapper.toResponse(
                        any(RecurringTransaction.class)
                )
        ).thenReturn(expectedResponse);

        RecurringTransactionResponse result =
                recurringTransactionService
                        .createRecurringTransaction(
                                userId,
                                request
                        );

        assertThat(result)
                .isEqualTo(expectedResponse);

        ArgumentCaptor<RecurringTransaction> captor =
                ArgumentCaptor.forClass(
                        RecurringTransaction.class
                );

        verify(recurringTransactionRepository)
                .save(captor.capture());

        RecurringTransaction saved =
                captor.getValue();

        assertThat(saved.getUserId())
                .isEqualTo(userId);

        assertThat(saved.getAccount())
                .isEqualTo(account);

        assertThat(saved.getCategory())
                .isEqualTo(category);

        assertThat(saved.getType())
                .isEqualTo(TransactionType.EXPENSE);

        assertThat(saved.getAmount())
                .isEqualByComparingTo("8000.00");

        assertThat(saved.getDescription())
                .isEqualTo("Monthly rent");

        assertThat(saved.getFrequency())
                .isEqualTo(
                        RecurrenceFrequency.MONTHLY
                );

        assertThat(saved.getStatus())
                .isEqualTo(
                        RecurringTransactionStatus.ACTIVE
                );

        assertThat(saved.getStartDate())
                .isEqualTo(startDate);

        assertThat(saved.getNextExecutionDate())
                .isEqualTo(startDate);

        assertThat(saved.getLastExecutionDate())
                .isNull();
    }

    @Test
    void shouldRejectCategoryTypeMismatch() {
        Long userId = 10L;
        Long accountId = 1L;
        Long categoryId = 2L;

        Account account =
                Account.builder()
                        .id(accountId)
                        .userId(userId)
                        .status(AccountStatus.ACTIVE)
                        .build();

        Category category =
                Category.builder()
                        .id(categoryId)
                        .userId(userId)
                        .name("Salary")
                        .type(TransactionType.INCOME)
                        .build();

        CreateRecurringTransactionRequest request =
                new CreateRecurringTransactionRequest(
                        accountId,
                        categoryId,
                        TransactionType.EXPENSE,
                        new BigDecimal("500.00"),
                        null,
                        RecurrenceFrequency.MONTHLY,
                        LocalDate.now(ZoneOffset.UTC),
                        null
                );

        when(
                accountRepository
                        .findByIdAndUserIdAndStatus(
                                accountId,
                                userId,
                                AccountStatus.ACTIVE
                        )
        ).thenReturn(Optional.of(account));

        when(
                categoryRepository.findByIdAndUserId(
                        categoryId,
                        userId
                )
        ).thenReturn(Optional.of(category));

        assertThatThrownBy(
                () -> recurringTransactionService
                        .createRecurringTransaction(
                                userId,
                                request
                        )
        )
                .isInstanceOf(
                        RecurringTransactionCategoryMismatchException.class
                );

        verify(
                recurringTransactionRepository,
                never()
        ).save(any(RecurringTransaction.class));
    }

    @Test
    void shouldReturnRecurringTransactions() {
        Long userId = 10L;

        RecurringTransaction recurringTransaction =
                RecurringTransaction.builder()
                        .id(1L)
                        .userId(userId)
                        .status(
                                RecurringTransactionStatus.ACTIVE
                        )
                        .build();

        RecurringTransactionResponse response =
                mock(RecurringTransactionResponse.class);

        when(
                recurringTransactionRepository
                        .findAllByUserIdOrderByCreatedAtDesc(
                                userId
                        )
        ).thenReturn(
                List.of(recurringTransaction)
        );

        when(
                recurringTransactionMapper.toResponse(
                        recurringTransaction
                )
        ).thenReturn(response);

        List<RecurringTransactionResponse> result =
                recurringTransactionService
                        .getRecurringTransactions(userId);

        assertThat(result)
                .containsExactly(response);
    }

    @Test
    void shouldPauseActiveRecurringTransaction() {
        Long userId = 10L;
        Long recurringTransactionId = 5L;

        RecurringTransaction recurringTransaction =
                RecurringTransaction.builder()
                        .id(recurringTransactionId)
                        .userId(userId)
                        .status(
                                RecurringTransactionStatus.ACTIVE
                        )
                        .build();

        RecurringTransactionResponse response =
                mock(RecurringTransactionResponse.class);

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
                recurringTransactionRepository.save(
                        recurringTransaction
                )
        ).thenReturn(recurringTransaction);

        when(
                recurringTransactionMapper.toResponse(
                        recurringTransaction
                )
        ).thenReturn(response);

        RecurringTransactionResponse result =
                recurringTransactionService
                        .pauseRecurringTransaction(
                                userId,
                                recurringTransactionId
                        );

        assertThat(result)
                .isEqualTo(response);

        assertThat(recurringTransaction.getStatus())
                .isEqualTo(
                        RecurringTransactionStatus.PAUSED
                );

        verify(recurringTransactionRepository)
                .save(recurringTransaction);
    }

    @Test
    void shouldResumePausedRecurringTransaction() {
        Long userId = 10L;
        Long recurringTransactionId = 5L;

        LocalDate today =
                LocalDate.now(ZoneOffset.UTC);

        RecurringTransaction recurringTransaction =
                RecurringTransaction.builder()
                        .id(recurringTransactionId)
                        .userId(userId)
                        .frequency(
                                RecurrenceFrequency.WEEKLY
                        )
                        .status(
                                RecurringTransactionStatus.PAUSED
                        )
                        .startDate(
                                today.minusWeeks(3)
                        )
                        .nextExecutionDate(
                                today.minusWeeks(2)
                        )
                        .build();

        RecurringTransactionResponse response =
                mock(RecurringTransactionResponse.class);

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
                recurringTransactionRepository.save(
                        recurringTransaction
                )
        ).thenReturn(recurringTransaction);

        when(
                recurringTransactionMapper.toResponse(
                        recurringTransaction
                )
        ).thenReturn(response);

        RecurringTransactionResponse result =
                recurringTransactionService
                        .resumeRecurringTransaction(
                                userId,
                                recurringTransactionId
                        );

        assertThat(result)
                .isEqualTo(response);

        assertThat(recurringTransaction.getStatus())
                .isEqualTo(
                        RecurringTransactionStatus.ACTIVE
                );

        assertThat(
                recurringTransaction
                        .getNextExecutionDate()
        ).isAfterOrEqualTo(today);

        verify(recurringTransactionRepository)
                .save(recurringTransaction);
    }

    @Test
    void shouldCancelRecurringTransaction() {
        Long userId = 10L;
        Long recurringTransactionId = 5L;

        RecurringTransaction recurringTransaction =
                RecurringTransaction.builder()
                        .id(recurringTransactionId)
                        .userId(userId)
                        .status(
                                RecurringTransactionStatus.ACTIVE
                        )
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

        recurringTransactionService
                .cancelRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        assertThat(recurringTransaction.getStatus())
                .isEqualTo(
                        RecurringTransactionStatus.CANCELLED
                );

        verify(recurringTransactionRepository)
                .save(recurringTransaction);
    }

    @Test
    void shouldNotSaveWhenCancelledTransactionIsCancelledAgain() {
        Long userId = 10L;
        Long recurringTransactionId = 5L;

        RecurringTransaction recurringTransaction =
                RecurringTransaction.builder()
                        .id(recurringTransactionId)
                        .userId(userId)
                        .status(
                                RecurringTransactionStatus.CANCELLED
                        )
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

        recurringTransactionService
                .cancelRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        verify(
                recurringTransactionRepository,
                never()
        ).save(any(RecurringTransaction.class));
    }
}