package com.smartwallet.financeservice.repository;

import com.smartwallet.financeservice.entity.FinancialTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, Long> {

    List<FinancialTransaction>
    findAllByUserIdOrderByTransactionDateDesc(Long userId);
    Optional<FinancialTransaction> findByIdAndUserId(
            Long transactionId,
            Long userId
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT transaction
            FROM FinancialTransaction transaction
            WHERE transaction.id = :transactionId
              AND transaction.userId = :userId
            """)
    Optional<FinancialTransaction> findOwnedTransactionForUpdate(
            @Param("transactionId") Long transactionId,
            @Param("userId") Long userId
    );
}