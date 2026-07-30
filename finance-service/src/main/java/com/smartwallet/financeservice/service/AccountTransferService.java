package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.CreateTransferRequest;
import com.smartwallet.financeservice.dto.response.TransferResponse;
import com.smartwallet.financeservice.entity.Account;
import com.smartwallet.financeservice.entity.AccountTransfer;
import com.smartwallet.financeservice.exception.AccountNotFoundException;
import com.smartwallet.financeservice.exception.InsufficientBalanceException;
import com.smartwallet.financeservice.exception.InvalidTransferAccountException;
import com.smartwallet.financeservice.exception.TransferCurrencyMismatchException;
import com.smartwallet.financeservice.mapper.AccountTransferMapper;
import com.smartwallet.financeservice.repository.AccountRepository;
import com.smartwallet.financeservice.repository.AccountTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccountTransferService {
    private final AccountTransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final AccountTransferMapper transferMapper;

    @Transactional
    public TransferResponse createTransfer(
            Long userId,
            CreateTransferRequest request
    ) {
        validateDifferentAccounts(
                request.fromAccountId(),
                request.toAccountId()
        );

        AccountPair accountPair = lockAccounts(
                userId,
                request.fromAccountId(),
                request.toAccountId()
        );

        Account fromAccount = accountPair.fromAccount();
        Account toAccount = accountPair.toAccount();

        validateSameCurrency(
                fromAccount,
                toAccount
        );

        validateSufficientBalance(
                fromAccount,
                request
        );

        fromAccount.setBalance(
                fromAccount.getBalance()
                        .subtract(request.amount())
        );
        toAccount.setBalance(
                toAccount.getBalance()
                        .add(request.amount())
        );

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        AccountTransfer transfer =
                AccountTransfer.builder()
                        .userId(userId)
                        .fromAccount(fromAccount)
                        .toAccount(toAccount)
                        .amount(request.amount())
                        .currency(fromAccount.getCurrency())
                        .description(
                                normalizeDescription(
                                        request.description()
                                )
                        )
                        .transferredAt(
                                request.transferredAt() != null
                                        ? request.transferredAt()
                                        : Instant.now()
                        )
                        .build();

        AccountTransfer savedTransfer =
                transferRepository.save(transfer);

        return transferMapper.toResponse(savedTransfer);
    }

    private AccountPair lockAccounts(
            Long userId,
            Long fromAccountId,
            Long toAccountId
    ) {
        Long firstAccountId =
                Math.min(
                        fromAccountId,
                        toAccountId
                );

        Long secondAccountId =
                Math.max(
                        fromAccountId,
                        toAccountId
                );

        Account firstAccount =
                lockAccount(
                        userId,
                        firstAccountId
                );

        Account secondAccount =
                lockAccount(
                        userId,
                        secondAccountId
                );

        Account fromAccount =
                fromAccountId.equals(firstAccountId)
                        ? firstAccount
                        : secondAccount;

        Account toAccount =
                toAccountId.equals(firstAccountId)
                        ? firstAccount
                        : secondAccount;

        return new AccountPair(
                fromAccount,
                toAccount
        );
    }

    private Account lockAccount(
            Long userId,
            Long accountId
    ) {
        return accountRepository
                .findOwnedAccountForUpdate(
                        accountId,
                        userId
                )
                .orElseThrow(
                        () -> new AccountNotFoundException(
                                accountId
                        )
                );
    }

    private record AccountPair(
            Account fromAccount,
            Account toAccount
    ) {
    }


    private void validateDifferentAccounts(
            Long fromAccountId,
            Long toAccountId
    ) {
        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidTransferAccountException();
        }

    }

    private void validateSameCurrency(
            Account fromAccount,
            Account toAccount
    ) {
        if (fromAccount.getCurrency()
                != toAccount.getCurrency()) {
            throw new TransferCurrencyMismatchException();
        }
    }

    private void validateSufficientBalance(
            Account fromAccount,
            CreateTransferRequest request
    ) {
        if (fromAccount
                .getBalance()
                .compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException();
        }

    }

    private String normalizeDescription(
            String description
    ) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}
