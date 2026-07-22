package com.smartwallet.budgetservice.repository;

import com.smartwallet.budgetservice.entity.ProcessedTransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProcessedTransactionEventRepository
        extends JpaRepository<ProcessedTransactionEvent, UUID> {

    @Modifying
    @Query(
            value = """
                    INSERT INTO processed_transaction_events
                        (event_id, event_type, processed_at)
                    VALUES
                        (:eventId, :eventType, CURRENT_TIMESTAMP)
                    ON CONFLICT (event_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("eventType") String eventType
    );
}