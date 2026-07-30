package com.smartwallet.financeservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "account_transfers",
        indexes = {
                @Index(
                        name = "idx_account_transfers_user_date",
                        columnList = "user_id, transferred_at"
                ),
                @Index(
                        name = "idx_account_transfers_from_account",
                        columnList = "from_account_id"
                ),
                @Index(
                        name = "idx_account_transfers_to_account",
                        columnList = "to_account_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "from_account_id",
            nullable = false
    )
    private Account fromAccount;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "to_account_id",
            nullable = false
    )
    private Account toAccount;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 3
    )
    private CurrencyCode currency;

    @Column(length = 255)
    private String description;

    @Column(
            name = "transferred_at",
            nullable = false
    )
    private Instant transferredAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (transferredAt == null) {
            transferredAt = now;
        }

        createdAt = now;
    }
}