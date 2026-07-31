package com.smartwallet.financeservice.repository;

import com.smartwallet.financeservice.entity.Account;
import com.smartwallet.financeservice.entity.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository
        extends JpaRepository<Account, Long> {

    List<Account> findAllByUserIdOrderByCreatedAtDesc(
            Long userId
    );


    List<Account> findAllByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            AccountStatus status
    );

    Optional<Account> findByIdAndUserIdAndStatus(
            Long accountId,
            Long userId,
            AccountStatus status
    );

    Optional<Account> findByIdAndUserId(
            Long accountId,
            Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT account
            FROM Account account
            WHERE account.id = :accountId
              AND account.userId = :userId
              AND account.status = :status
            """)
    Optional<Account> findOwnedAccountForUpdate(
            @Param("accountId") Long accountId,
            @Param("userId") Long userId,
            @Param("status") AccountStatus status
    );
}