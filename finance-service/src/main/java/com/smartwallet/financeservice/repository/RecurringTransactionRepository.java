package com.smartwallet.financeservice.repository;

import com.smartwallet.financeservice.entity.RecurringTransaction;
import com.smartwallet.financeservice.entity.RecurringTransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository
        extends JpaRepository<RecurringTransaction, Long> {


    List<RecurringTransaction>
    findAllByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    Optional<RecurringTransaction>
    findByIdAndUserId(
            Long recurringTransactionId,
            Long userId
    );

    @Query("""
            SELECT recurringTransaction.id
            FROM RecurringTransaction recurringTransaction
            WHERE recurringTransaction.status = :status
              AND recurringTransaction.nextExecutionDate <= :executionDate
            ORDER BY recurringTransaction.nextExecutionDate ASC,
                     recurringTransaction.id ASC
            """)
    List<Long> findDueRecurringTransactionIds(
            @Param("status")
            RecurringTransactionStatus status,

            @Param("executionDate")
            LocalDate executionDate,

            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT recurringTransaction
            FROM RecurringTransaction recurringTransaction
            WHERE recurringTransaction.id = :recurringTransactionId
            """)
    Optional<RecurringTransaction>
    findByIdForUpdate(
            @Param("recurringTransactionId")
            Long recurringTransactionId
    );
}