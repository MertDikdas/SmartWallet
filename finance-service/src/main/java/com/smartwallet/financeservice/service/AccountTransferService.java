package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.request.CreateTransferRequest;
import com.smartwallet.financeservice.dto.request.TransferFilterRequest;
import com.smartwallet.financeservice.dto.response.PageResponse;
import com.smartwallet.financeservice.dto.response.TransferResponse;
import com.smartwallet.financeservice.entity.Account;
import com.smartwallet.financeservice.entity.AccountStatus;
import com.smartwallet.financeservice.entity.AccountTransfer;
import com.smartwallet.financeservice.exception.*;
import com.smartwallet.financeservice.mapper.AccountTransferMapper;
import com.smartwallet.financeservice.repository.AccountRepository;
import com.smartwallet.financeservice.repository.AccountTransferRepository;
import com.smartwallet.financeservice.specification.AccountTransferSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountTransferService {
    private final AccountTransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final AccountTransferMapper transferMapper;

    @Transactional
    public TransferResponse createTransfer(
            Long userId,
            String idempotencyKey,
            CreateTransferRequest request
    ) {
        String normalizedIdempotencyKey =
                normalizeIdempotencyKey(idempotencyKey);

        validateDifferentAccounts(
                request.fromAccountId(),
                request.toAccountId()
        );

        String requestFingerprint =
                createRequestFingerprint(request);

        Optional<TransferResponse> existingResponse =
                findExistingIdempotentResponse(
                        userId,
                        normalizedIdempotencyKey,
                        requestFingerprint
                );

        if (existingResponse.isPresent()) {
            return existingResponse.get();
        }

        AccountPair accounts = lockAccounts(
                userId,
                request.fromAccountId(),
                request.toAccountId()
        );

        existingResponse =
                findExistingIdempotentResponse(
                        userId,
                        normalizedIdempotencyKey,
                        requestFingerprint
                );

        if (existingResponse.isPresent()) {
            return existingResponse.get();
        }

        Account fromAccount = accounts.fromAccount();
        Account toAccount = accounts.toAccount();

        validateSameCurrency(
                fromAccount,
                toAccount
        );

        validateSufficientBalance(
                fromAccount,
                request
        );

        fromAccount.setBalance(
                fromAccount
                        .getBalance()
                        .subtract(request.amount())
        );

        toAccount.setBalance(
                toAccount
                        .getBalance()
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
                        .idempotencyKey(
                                normalizedIdempotencyKey
                        )
                        .requestFingerprint(
                                requestFingerprint
                        )
                        .transferredAt(
                                request.transferredAt() != null
                                        ? request.transferredAt()
                                        : Instant.now()
                        )
                        .build();

        try {
            AccountTransfer savedTransfer =
                    transferRepository.saveAndFlush(transfer);

            return transferMapper.toResponse(savedTransfer);

        } catch (DataIntegrityViolationException exception) {
            throw new IdempotencyConflictException();
        }
    }


    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> getTransfers(
            Long userId,
            TransferFilterRequest filter
    ){
        Pageable pageable = PageRequest.of(
                filter.resolvedPage(),
                filter.resolvedSize(),
                Sort.by(
                        Sort.Direction.DESC,
                        "transferredAt"
                ).and(
                        Sort.by(
                                Sort.Direction.DESC,
                                "id"
                        )
                )
        );

        Page<TransferResponse> transferPage =
                transferRepository.findAll(
                        AccountTransferSpecification
                                .withFilters(
                                        userId,
                                        filter
                                ),
                        pageable
                ).map(
                        transferMapper::toResponse
                );
        return PageResponse.from(transferPage);
    }

    private String createRequestFingerprint(
            CreateTransferRequest request
    ) {
        String normalizedDescription =
                normalizeDescription(
                        request.description()
                );

        String normalizedAmount =
                request.amount() != null
                        ? request.amount()
                        .stripTrailingZeros()
                        .toPlainString()
                        : "null";

        String transferredAtValue =
                request.transferredAt() != null
                        ? request.transferredAt().toString()
                        : "null";

        String canonicalRequest =
                String.join(
                        "|",
                        String.valueOf(request.fromAccountId()),
                        String.valueOf(request.toAccountId()),
                        normalizedAmount,
                        normalizedDescription != null
                                ? normalizedDescription
                                : "null",
                        transferredAtValue
                );

        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    messageDigest.digest(
                            canonicalRequest.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    private String normalizeIdempotencyKey(
            String idempotencyKey
    ) {
        if (idempotencyKey == null) {
            throw new InvalidIdempotencyKeyException();
        }

        String normalized =
                idempotencyKey.trim();

        if (normalized.isEmpty()
                || normalized.length() > 100) {
            throw new InvalidIdempotencyKeyException();
        }

        return normalized;
    }

    private Optional<TransferResponse>
    findExistingIdempotentResponse(
            Long userId,
            String idempotencyKey,
            String requestFingerprint
    ) {
        return transferRepository
                .findByUserIdAndIdempotencyKey(
                        userId,
                        idempotencyKey
                )
                .map(existingTransfer -> {
                    if (!existingTransfer
                            .getRequestFingerprint()
                            .equals(requestFingerprint)) {
                        throw new IdempotencyConflictException();
                    }

                    return transferMapper.toResponse(
                            existingTransfer
                    );
                });
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
                        userId,
                        AccountStatus.ACTIVE
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
