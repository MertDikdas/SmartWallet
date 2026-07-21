package com.smartwallet.financeservice.repository;

import com.smartwallet.financeservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

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
}