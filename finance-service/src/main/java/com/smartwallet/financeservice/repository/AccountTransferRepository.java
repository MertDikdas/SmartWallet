package com.smartwallet.financeservice.repository;

import com.smartwallet.financeservice.entity.AccountTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountTransferRepository extends JpaRepository<AccountTransfer, Long> {

    Optional<AccountTransfer> findByIdAndUserId(
            Long id,
            Long userId
    );
}
