package com.smartwallet.financeservice.repository;

import com.smartwallet.financeservice.entity.RecurringTransactionExecution;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionExecutionRepository
        extends JpaRepository<RecurringTransactionExecution, Long> {

    Optional<RecurringTransactionExecution>
    findByRecurringTransactionIdAndScheduledDate(
            Long recurringTransactionId,
            LocalDate scheduledDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT execution
            FROM RecurringTransactionExecution execution
            WHERE execution.recurringTransaction.id =
                    :recurringTransactionId
              AND execution.scheduledDate =
                    :scheduledDate
            """)
    Optional<RecurringTransactionExecution> findPeriodForUpdate(
            @Param("recurringTransactionId")
            Long recurringTransactionId,

            @Param("scheduledDate")
            LocalDate scheduledDate
    );

    List<RecurringTransactionExecution>
    findAllByRecurringTransactionIdOrderByScheduledDateDesc(
            Long recurringTransactionId
    );
}