package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.CreateAccountRequest;
import com.smartwallet.financeservice.dto.request.UpdateAccountRequest;
import com.smartwallet.financeservice.dto.response.AccountResponse;
import com.smartwallet.financeservice.entity.Account;
import com.smartwallet.financeservice.exception.AccountNotFoundException;
import com.smartwallet.financeservice.mapper.AccountMapper;
import com.smartwallet.financeservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse createAccount(
            Long userId,
            CreateAccountRequest request
    ) {
        Account account = Account.builder()
                .userId(userId)
                .name(request.name().trim())
                .type(request.type())
                .currency(request.currency())
                .balance(request.initialBalance())
                .build();

        Account savedAccount =
                accountRepository.save(account);

        return accountMapper.toResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(Long userId) {
        return accountRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(
            Long userId,
            Long accountId
    ) {
        Account account = findOwnedAccount(accountId, userId);

        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(
            Long userId,
            Long accountId,
            UpdateAccountRequest request
    ) {
        Account account = findOwnedAccount(accountId, userId);

        if (request.name() != null) {
            account.setName(request.name().trim());
        }

        if (request.type() != null) {
            account.setType(request.type());
        }

        if (request.currency() != null) {
            account.setCurrency(request.currency());
        }

        return accountMapper.toResponse(account);
    }

    @Transactional
    public void deleteAccount(
            Long userId,
            Long accountId
    ) {
        Account account = findOwnedAccount(accountId, userId);

        accountRepository.delete(account);
    }

    private Account findOwnedAccount(
            Long accountId,
            Long userId
    ) {
        return accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(
                        () -> new AccountNotFoundException(accountId)
                );
    }
}