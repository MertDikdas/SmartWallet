package com.smartwallet.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_transaction_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedTransactionEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 20
    )
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}