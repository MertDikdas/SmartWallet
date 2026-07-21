package com.smartwallet.financeservice.repository;

import com.smartwallet.financeservice.entity.Account;
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
        """)
    Optional<Account> findOwnedAccountForUpdate(
            @Param("accountId") Long accountId,
            @Param("userId") Long userId
    );
}