package com.smartwallet.financeservice.repository;

import com.smartwallet.financeservice.entity.AccountTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AccountTransferRepository
        extends JpaRepository<AccountTransfer, Long> ,
        JpaSpecificationExecutor<AccountTransfer> {

    Optional<AccountTransfer> findByIdAndUserId(
            Long id,
            Long userId
    );

    Optional<AccountTransfer> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
