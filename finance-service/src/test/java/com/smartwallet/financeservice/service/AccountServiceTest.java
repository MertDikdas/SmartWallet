package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.response.AccountResponse;
import com.smartwallet.financeservice.entity.Account;
import com.smartwallet.financeservice.entity.AccountStatus;
import com.smartwallet.financeservice.entity.AccountType;
import com.smartwallet.financeservice.entity.CurrencyCode;
import com.smartwallet.financeservice.exception.AccountBalanceNotZeroException;
import com.smartwallet.financeservice.exception.AccountNotFoundException;
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
import static org.mockito.Mockito.*;

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

    @Test
    void shouldReturnOnlyArchivedAccounts(){
        Long userId = 10L;

        Account account =
                Account.builder()
                        .id(1L)
                        .userId(userId)
                        .name("Main Account")
                        .type(AccountType.CHECKING)
                        .currency(CurrencyCode.TRY)
                        .balance(new BigDecimal("120.00"))
                        .status(AccountStatus.ARCHIVED)
                        .build();

        AccountResponse response = mock(AccountResponse.class);

        when(
                accountRepository.
                        findAllByUserIdAndStatusOrderByCreatedAtDesc(
                                userId,
                                AccountStatus.ARCHIVED
                        )
        ).thenReturn(List.of(account));
        when(
                accountMapper.toResponse(account)
        ).thenReturn(response);
        List<AccountResponse> archivedAccounts = accountService.getArchivedAccounts(userId);

        assertThat(archivedAccounts).containsExactly(response);
        verify(accountMapper).toResponse(account);
    }

    @Test
    public void shouldRestoreArchivedAccount() {
        Long userId = 10L;
        Long accountId = 5L;

        Account account = Account.builder()
                .id(accountId)
                .userId(userId)
                .name("Main Account")
                .type(AccountType.CHECKING)
                .currency(CurrencyCode.TRY)
                .balance(new BigDecimal("120.00"))
                .status(AccountStatus.ARCHIVED)
                .build();

        AccountResponse response = mock(AccountResponse.class);

        when(
                accountRepository.findByIdAndUserId(
                        accountId,
                        userId
                )
        ).thenReturn(Optional.of(account));

        when(
                accountRepository.save(account)
        ).thenReturn(account);

        when(
                accountMapper.toResponse(account)
        ).thenReturn(response);
        AccountResponse result = accountService.restoreAccount(userId, accountId);

        assertThat(result).isEqualTo(response);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);

        verify(accountRepository).save(account);

        verify(accountMapper).toResponse(account);

    }

    @Test
    void shouldReturnAccountWithoutSavingWhenAlreadyActive() {
        Long userId = 10L;
        Long accountId = 5L;

        Account activeAccount =
                Account.builder()
                        .id(accountId)
                        .userId(userId)
                        .name("Main Account")
                        .type(AccountType.CHECKING)
                        .currency(CurrencyCode.TRY)
                        .balance(BigDecimal.ZERO)
                        .status(AccountStatus.ACTIVE)
                        .build();

        AccountResponse response =
                mock(AccountResponse.class);

        when(
                accountRepository.findByIdAndUserId(
                        accountId,
                        userId
                )
        ).thenReturn(
                Optional.of(activeAccount)
        );

        when(
                accountMapper.toResponse(activeAccount)
        ).thenReturn(response);

        AccountResponse result =
                accountService.restoreAccount(
                        userId,
                        accountId
                );

        assertThat(result)
                .isEqualTo(response);

        assertThat(activeAccount.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);

        verify(accountRepository, never())
                .save(any(Account.class));

        verify(accountMapper)
                .toResponse(activeAccount);
    }

    @Test
    void shouldRejectRestoringAccountThatDoesNotBelongToUser() {
        Long userId = 10L;
        Long accountId = 99L;

        when(
                accountRepository.findByIdAndUserId(
                        accountId,
                        userId
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> accountService.restoreAccount(
                        userId,
                        accountId
                )
        )
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository, never())
                .save(any(Account.class));

        verifyNoInteractions(accountMapper);
    }
}
