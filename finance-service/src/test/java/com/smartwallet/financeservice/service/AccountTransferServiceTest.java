package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.CreateTransferRequest;
import com.smartwallet.financeservice.dto.request.TransferFilterRequest;
import com.smartwallet.financeservice.dto.response.PageResponse;
import com.smartwallet.financeservice.dto.response.TransferResponse;
import com.smartwallet.financeservice.entity.Account;
import com.smartwallet.financeservice.entity.AccountTransfer;
import com.smartwallet.financeservice.entity.AccountType;
import com.smartwallet.financeservice.entity.CurrencyCode;
import com.smartwallet.financeservice.exception.InsufficientBalanceException;
import com.smartwallet.financeservice.mapper.AccountTransferMapper;
import com.smartwallet.financeservice.repository.AccountRepository;
import com.smartwallet.financeservice.repository.AccountTransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountTransferServiceTest {

    @Mock
    private AccountTransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransferMapper transferMapper;

    @InjectMocks
    private AccountTransferService transferService;

    @Test
    void shouldTransferMoneyBetweenOwnedAccounts() {
        Long userId = 10L;

        Instant transferredAt =
                Instant.parse("2026-07-30T15:00:00Z");

        Account fromAccount =
                Account.builder()
                        .id(1L)
                        .userId(userId)
                        .name("Main Account")
                        .type(AccountType.CHECKING)
                        .balance(new BigDecimal("1000.00"))
                        .currency(CurrencyCode.TRY)
                        .build();

        Account toAccount =
                Account.builder()
                        .id(2L)
                        .userId(userId)
                        .name("Cash Account")
                        .type(AccountType.CASH)
                        .balance(new BigDecimal("200.00"))
                        .currency(CurrencyCode.TRY)
                        .build();

        CreateTransferRequest request =
                new CreateTransferRequest(
                        1L,
                        2L,
                        new BigDecimal("300.00"),
                        "Cash transfer",
                        transferredAt
                );

        TransferResponse expectedResponse =
                new TransferResponse(
                        50L,
                        1L,
                        "Main Account",
                        2L,
                        "Cash Account",
                        new BigDecimal("300.00"),
                        CurrencyCode.TRY,
                        "Cash transfer",
                        transferredAt,
                        Instant.parse("2026-07-30T15:00:01Z")
                );

        when(
                accountRepository.findOwnedAccountForUpdate(
                        1L,
                        userId
                )
        ).thenReturn(Optional.of(fromAccount));

        when(
                accountRepository.findOwnedAccountForUpdate(
                        2L,
                        userId
                )
        ).thenReturn(Optional.of(toAccount));

        when(
                transferRepository.save(
                        any(AccountTransfer.class)
                )
        ).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        when(
                transferMapper.toResponse(
                        any(AccountTransfer.class)
                )
        ).thenReturn(expectedResponse);

        TransferResponse result =
                transferService.createTransfer(
                        userId,
                        request
                );

        assertThat(result)
                .isEqualTo(expectedResponse);

        assertThat(fromAccount.getBalance())
                .isEqualByComparingTo("700.00");

        assertThat(toAccount.getBalance())
                .isEqualByComparingTo("500.00");

        verify(accountRepository)
                .save(fromAccount);

        verify(accountRepository)
                .save(toAccount);

        ArgumentCaptor<AccountTransfer> transferCaptor =
                ArgumentCaptor.forClass(
                        AccountTransfer.class
                );

        verify(transferRepository)
                .save(transferCaptor.capture());

        AccountTransfer savedTransfer =
                transferCaptor.getValue();

        assertThat(savedTransfer.getUserId())
                .isEqualTo(userId);

        assertThat(savedTransfer.getFromAccount())
                .isEqualTo(fromAccount);

        assertThat(savedTransfer.getToAccount())
                .isEqualTo(toAccount);

        assertThat(savedTransfer.getAmount())
                .isEqualByComparingTo("300.00");

        assertThat(savedTransfer.getCurrency())
                .isEqualTo(CurrencyCode.TRY);

        assertThat(savedTransfer.getTransferredAt())
                .isEqualTo(transferredAt);
    }

    @Test
    void shouldRejectTransferWhenBalanceIsInsufficient() {
        Long userId = 10L;

        Account fromAccount =
                Account.builder()
                        .id(1L)
                        .userId(userId)
                        .name("Main Account")
                        .type(AccountType.CHECKING)
                        .balance(new BigDecimal("100.00"))
                        .currency(CurrencyCode.TRY)
                        .build();

        Account toAccount =
                Account.builder()
                        .id(2L)
                        .userId(userId)
                        .name("Cash Account")
                        .type(AccountType.CASH)
                        .balance(new BigDecimal("200.00"))
                        .currency(CurrencyCode.TRY)
                        .build();

        CreateTransferRequest request =
                new CreateTransferRequest(
                        1L,
                        2L,
                        new BigDecimal("300.00"),
                        null,
                        null
                );

        when(
                accountRepository.findOwnedAccountForUpdate(
                        1L,
                        userId
                )
        ).thenReturn(Optional.of(fromAccount));

        when(
                accountRepository.findOwnedAccountForUpdate(
                        2L,
                        userId
                )
        ).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(
                () -> transferService.createTransfer(
                        userId,
                        request
                )
        )
                .isInstanceOf(
                        InsufficientBalanceException.class
                )
                .hasMessageContaining(
                        "Source account does not have sufficient balance"
                );

        assertThat(fromAccount.getBalance())
                .isEqualByComparingTo("100.00");

        assertThat(toAccount.getBalance())
                .isEqualByComparingTo("200.00");

        verify(accountRepository, never())
                .save(any(Account.class));

        verify(transferRepository, never())
                .save(any(AccountTransfer.class));
    }

    @Test
    void shouldReturnTransferHistoryWithPagination() {
        Long userId = 10L;

        Instant transferredAt =
                Instant.parse("2026-07-20T12:00:00Z");

        Instant createdAt =
                Instant.parse("2026-07-20T12:00:01Z");

        Account fromAccount =
                Account.builder()
                        .id(1L)
                        .userId(userId)
                        .name("Main Account")
                        .type(AccountType.CHECKING)
                        .balance(new BigDecimal("700.00"))
                        .currency(CurrencyCode.TRY)
                        .build();

        Account toAccount =
                Account.builder()
                        .id(2L)
                        .userId(userId)
                        .name("Cash Account")
                        .type(AccountType.CASH)
                        .balance(new BigDecimal("500.00"))
                        .currency(CurrencyCode.TRY)
                        .build();

        AccountTransfer transfer =
                AccountTransfer.builder()
                        .id(50L)
                        .userId(userId)
                        .fromAccount(fromAccount)
                        .toAccount(toAccount)
                        .amount(new BigDecimal("300.00"))
                        .currency(CurrencyCode.TRY)
                        .description("Cash transfer")
                        .transferredAt(transferredAt)
                        .createdAt(createdAt)
                        .build();

        TransferResponse transferResponse =
                new TransferResponse(
                        50L,
                        1L,
                        "Main Account",
                        2L,
                        "Cash Account",
                        new BigDecimal("300.00"),
                        CurrencyCode.TRY,
                        "Cash transfer",
                        transferredAt,
                        createdAt
                );

        TransferFilterRequest filter =
                new TransferFilterRequest(
                        1L,
                        Instant.parse("2026-07-01T00:00:00Z"),
                        Instant.parse("2026-07-31T23:59:59Z"),
                        1,
                        10
                );

        when(
                transferRepository.findAll(
                        any(Specification.class),
                        any(Pageable.class)
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(transfer)
                )
        );

        when(
                transferMapper.toResponse(transfer)
        ).thenReturn(transferResponse);

        PageResponse<TransferResponse> result =
                transferService.getTransfers(
                        userId,
                        filter
                );

        assertThat(result.content())
                .containsExactly(transferResponse);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(transferRepository).findAll(
                any(Specification.class),
                pageableCaptor.capture()
        );

        Pageable pageable =
                pageableCaptor.getValue();

        assertThat(pageable.getPageNumber())
                .isEqualTo(1);

        assertThat(pageable.getPageSize())
                .isEqualTo(10);

        assertThat(
                pageable.getSort()
                        .getOrderFor("transferredAt")
        )
                .isNotNull()
                .satisfies(
                        order -> assertThat(
                                order.isDescending()
                        ).isTrue()
                );

        assertThat(
                pageable.getSort()
                        .getOrderFor("id")
        )
                .isNotNull()
                .satisfies(
                        order -> assertThat(
                                order.isDescending()
                        ).isTrue()
                );
    }
}