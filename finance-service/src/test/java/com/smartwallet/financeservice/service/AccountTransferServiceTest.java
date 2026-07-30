package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.CreateTransferRequest;
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

import java.math.BigDecimal;
import java.time.Instant;
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
}