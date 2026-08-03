package com.smartwallet.financeservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "recurring_transaction_executions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recurring_execution_period",
                        columnNames = {
                                "recurring_transaction_id",
                                "scheduled_date"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_recurring_executions_recurring",
                        columnList = "recurring_transaction_id,scheduled_date"
                ),
                @Index(
                        name = "idx_recurring_executions_status",
                        columnList = "status,created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "recurring_transaction_id",
            nullable = false
    )
    private RecurringTransaction recurringTransaction;

    @Column(
            name = "scheduled_date",
            nullable = false
    )
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private RecurringExecutionStatus status;

    @Column(name = "generated_transaction_id")
    private Long generatedTransactionId;

    @Column(
            name = "error_message",
            length = 500
    )
    private String errorMessage;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}