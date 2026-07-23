package com.smartwallet.analyticsservice.entity;

import com.smartwallet.contracts.transaction.TransactionSnapshot;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_projections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionProjection {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "transaction_type",
            nullable = false,
            length = 20
    )
    private ProjectionTransactionType transactionType;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static TransactionProjection from(
            TransactionSnapshot snapshot
    ) {
        return TransactionProjection.builder()
                .transactionId(snapshot.transactionId())
                .userId(snapshot.userId())
                .accountId(snapshot.accountId())
                .categoryId(snapshot.categoryId())
                .transactionType(
                        ProjectionTransactionType.valueOf(
                                snapshot.transactionType()
                        )
                )
                .amount(snapshot.amount())
                .transactionDate(snapshot.transactionDate())
                .updatedAt(Instant.now())
                .build();
    }

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}