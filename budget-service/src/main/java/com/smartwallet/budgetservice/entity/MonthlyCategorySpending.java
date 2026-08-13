package com.smartwallet.budgetservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "monthly_category_spending",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_monthly_category_spending_user_category_period",
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
public class MonthlyCategorySpending {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

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


        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

}