package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.TransactionFilterRequest;
import com.smartwallet.financeservice.dto.response.PageResponse;
import com.smartwallet.financeservice.dto.response.TransactionResponse;
import com.smartwallet.financeservice.entity.FinancialTransaction;
import com.smartwallet.financeservice.entity.TransactionType;
import com.smartwallet.financeservice.mapper.TransactionMapper;
import com.smartwallet.financeservice.outbox.OutboxEventService;
import com.smartwallet.financeservice.repository.AccountRepository;
import com.smartwallet.financeservice.repository.CategoryRepository;
import com.smartwallet.financeservice.repository.FinancialTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private FinancialTransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private OutboxEventService outboxEventService;

    @Test
    void shouldReturnFilteredTransactionsWithPagination() {
        Long userId = 10L;

        Instant transactionDate =
                Instant.parse("2026-07-15T10:00:00Z");

        FinancialTransaction transaction =
                FinancialTransaction.builder()
                        .id(1L)
                        .userId(userId)
                        .type(TransactionType.EXPENSE)
                        .amount(new BigDecimal("150.00"))
                        .description("Market")
                        .transactionDate(transactionDate)
                        .createdAt(transactionDate)
                        .build();

        TransactionResponse transactionResponse =
                new TransactionResponse(
                        1L,
                        5L,
                        7L,
                        "Market",
                        TransactionType.EXPENSE,
                        new BigDecimal("150.00"),
                        "Market",
                        transactionDate,
                        transactionDate
                );

        TransactionFilterRequest filter =
                new TransactionFilterRequest(
                        5L,
                        7L,
                        TransactionType.EXPENSE,
                        Instant.parse("2026-07-01T00:00:00Z"),
                        Instant.parse("2026-07-31T23:59:59Z"),
                        1,
                        10
                );

        when(
                transactionRepository.findAll(
                        ArgumentMatchers.<Specification<FinancialTransaction>>any(),
                        any(Pageable.class)
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(transaction)
                )
        );

        when(
                transactionMapper.toResponse(transaction)
        ).thenReturn(transactionResponse);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        accountRepository,
                        categoryRepository,
                        transactionMapper,
                        outboxEventService
                );

        PageResponse<TransactionResponse> result =
                transactionService.getTransactions(
                        userId,
                        filter
                );

        assertThat(result.content())
                .containsExactly(transactionResponse);

        assertThat(result.content())
                .hasSize(1);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(transactionRepository).findAll(
                ArgumentMatchers.<Specification<FinancialTransaction>>any(),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber())
                .isEqualTo(1);

        assertThat(pageable.getPageSize())
                .isEqualTo(10);

        assertThat(
                pageable.getSort()
                        .getOrderFor("transactionDate")
        )
                .isNotNull()
                .satisfies(
                        order -> assertThat(order.isDescending())
                                .isTrue()
                );

        assertThat(
                pageable.getSort()
                        .getOrderFor("id")
        )
                .isNotNull()
                .satisfies(
                        order -> assertThat(order.isDescending())
                                .isTrue()
                );
    }
    @Test
    void shouldUseDefaultPaginationWhenPageAndSizeAreMissing() {
        Long userId = 10L;

        TransactionFilterRequest filter =
                new TransactionFilterRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(
                transactionRepository.findAll(
                        ArgumentMatchers.<Specification<FinancialTransaction>>any(),
                        any(Pageable.class)
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        accountRepository,
                        categoryRepository,
                        transactionMapper,
                        outboxEventService
                );

        transactionService.getTransactions(
                userId,
                filter
        );

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(transactionRepository).findAll(
                ArgumentMatchers.<Specification<FinancialTransaction>>any(),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber())
                .isZero();

        assertThat(pageable.getPageSize())
                .isEqualTo(20);
    }
}