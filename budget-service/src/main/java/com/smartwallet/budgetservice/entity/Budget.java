package com.smartwallet.budgetservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "budgets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_budgets_user_category_period",
                        columnNames = {
                                "user_id",
                                "category_id",
                                "year",
                                "month"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(
            name = "limit_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal limitAmount;

    @Column(
            name = "spent_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal spentAmount;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (spentAmount == null) {
            spentAmount = BigDecimal.ZERO;
        }

        recalculateStatus();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        recalculateStatus();
        updatedAt = Instant.now();
    }

    public void recalculateStatus() {
        if (spentAmount == null || limitAmount == null) {
            status = BudgetStatus.ACTIVE;
            return;
        }

        status = spentAmount.compareTo(limitAmount) > 0
                ? BudgetStatus.EXCEEDED
                : BudgetStatus.ACTIVE;
    }
}