package com.smartwallet.financeservice.specification;

import com.smartwallet.financeservice.dto.request.TransactionFilterRequest;
import com.smartwallet.financeservice.entity.FinancialTransaction;
import com.smartwallet.financeservice.entity.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class TransactionSpecification {

    private TransactionSpecification() {
    }

    public static Specification<FinancialTransaction> withFilters(
            Long userId,
            TransactionFilterRequest filter
    ) {
        List<Specification<FinancialTransaction>> specifications =
                new ArrayList<>();

        specifications.add(hasUserId(userId));

        if (filter.accountId() != null) {
            specifications.add(
                    hasAccountId(filter.accountId())
            );
        }

        if (filter.categoryId() != null) {
            specifications.add(
                    hasCategoryId(filter.categoryId())
            );
        }

        if (filter.type() != null) {
            specifications.add(
                    hasType(filter.type())
            );
        }

        if (filter.startDate() != null) {
            specifications.add(
                    transactionDateAfterOrEqual(filter.startDate())
            );
        }

        if (filter.endDate() != null) {
            specifications.add(
                    transactionDateBeforeOrEqual(filter.endDate())
            );
        }

        return Specification.allOf(specifications);
    }

    private static Specification<FinancialTransaction> hasUserId(
            Long userId
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("userId"),
                        userId
                );
    }

    private static Specification<FinancialTransaction> hasAccountId(
            Long accountId
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("account").get("id"),
                        accountId
                );
    }

    private static Specification<FinancialTransaction> hasCategoryId(
            Long categoryId
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("category").get("id"),
                        categoryId
                );
    }

    private static Specification<FinancialTransaction> hasType(
            TransactionType type
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("type"),
                        type
                );
    }

    private static Specification<FinancialTransaction>
    transactionDateAfterOrEqual(
            java.time.Instant startDate
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(
                        root.get("transactionDate"),
                        startDate
                );
    }

    private static Specification<FinancialTransaction>
    transactionDateBeforeOrEqual(
            java.time.Instant endDate
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(
                        root.get("transactionDate"),
                        endDate
                );
    }
}