package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.entity.Account;
import com.smartwallet.financeservice.entity.AccountStatus;
import com.smartwallet.financeservice.entity.AccountType;
import com.smartwallet.financeservice.entity.CurrencyCode;
import com.smartwallet.financeservice.exception.AccountBalanceNotZeroException;
import com.smartwallet.financeservice.mapper.AccountMapper;
import com.smartwallet.financeservice.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    @Test
    public void shouldArchiveAccountWhenBalanceIsZero(){
        Long userId = 10L;
        Long accountId = 1L;

        Account account = Account.builder()
                .id(accountId)
                .userId(userId)
                .status(AccountStatus.ACTIVE)
                .balance(java.math.BigDecimal.ZERO)
                .type(AccountType.CHECKING)
                .name("Test Account")
                .currency(CurrencyCode.TRY)
                .build();

        when(
                accountRepository
                        .findByIdAndUserId(
                                accountId,
                                userId
                        )
        ).thenReturn(Optional.of(account));

        accountService.deleteAccount(userId, accountId);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ARCHIVED);

        verify(accountRepository).save(account);


    }

    @Test
    void shouldRejectArchivingWhenBalanceIsNotZero() {
        Long userId = 10L;
        Long accountId = 1L;

        Account account =
                Account.builder()
                        .id(accountId)
                        .userId(userId)
                        .name("Main Account")
                        .type(AccountType.CHECKING)
                        .currency(CurrencyCode.TRY)
                        .balance(new BigDecimal("125.50"))
                        .status(AccountStatus.ACTIVE)
                        .build();

        when(
                accountRepository.findByIdAndUserId(
                        accountId,
                        userId
                )
        ).thenReturn(Optional.of(account));

        assertThatThrownBy(
                () -> accountService.deleteAccount(
                        userId,
                        accountId
                )
        )
                .isInstanceOf(
                        AccountBalanceNotZeroException.class
                )
                .hasMessageContaining(
                        "balance must be zero"
                );

        assertThat(account.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);

        verify(accountRepository, never())
                .save(any(Account.class));
    }

    @Test
    void shouldDoNothingWhenAccountIsAlreadyArchived() {
        Long userId = 10L;
        Long accountId = 1L;

        Account account =
                Account.builder()
                        .id(accountId)
                        .userId(userId)
                        .name("Archived Account")
                        .type(AccountType.CHECKING)
                        .currency(CurrencyCode.TRY)
                        .balance(BigDecimal.ZERO)
                        .status(AccountStatus.ARCHIVED)
                        .build();

        when(
                accountRepository.findByIdAndUserId(
                        accountId,
                        userId
                )
        ).thenReturn(Optional.of(account));

        accountService.deleteAccount(
                userId,
                accountId
        );

        assertThat(account.getStatus())
                .isEqualTo(AccountStatus.ARCHIVED);

        verify(accountRepository, never())
                .save(any(Account.class));
    }

    @Test
    void shouldRequestOnlyActiveAccountsWhenListingAccounts() {
        Long userId = 10L;

        when(
                accountRepository
                        .findAllByUserIdAndStatusOrderByCreatedAtDesc(
                                userId,
                                AccountStatus.ACTIVE
                        )
        ).thenReturn(List.of());

        var result =
                accountService.getAccounts(userId);

        assertThat(result)
                .isEmpty();

        verify(accountRepository)
                .findAllByUserIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        AccountStatus.ACTIVE
                );
    }

}
