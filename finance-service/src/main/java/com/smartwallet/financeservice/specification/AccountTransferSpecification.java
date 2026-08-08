package com.smartwallet.financeservice.specification;

import com.smartwallet.financeservice.dto.request.TransferFilterRequest;
import com.smartwallet.financeservice.entity.AccountTransfer;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AccountTransferSpecification {

    private AccountTransferSpecification() {
    }

    public static Specification<AccountTransfer> withFilters(
            Long userId,
            TransferFilterRequest filter
    ) {
        List<Specification<AccountTransfer>> specifications =
                new ArrayList<>();

        specifications.add(
                hasUserId(userId)
        );

        if (filter.accountId() != null) {
            specifications.add(
                    involvesAccount(filter.accountId())
            );
        }

        if (filter.startDate() != null) {
            specifications.add(
                    transferredAtAfterOrEqual(
                            filter.startDate()
                    )
            );
        }

        if (filter.endDate() != null) {
            specifications.add(
                    transferredAtBeforeOrEqual(
                            filter.endDate()
                    )
            );
        }

        return Specification.allOf(specifications);
    }

    private static Specification<AccountTransfer> hasUserId(
            Long userId
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("userId"),
                        userId
                );
    }

    private static Specification<AccountTransfer> involvesAccount(
            Long accountId
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.equal(
                                root.get("fromAccount").get("id"),
                                accountId
                        ),
                        criteriaBuilder.equal(
                                root.get("toAccount").get("id"),
                                accountId
                        )
                );
    }

    private static Specification<AccountTransfer>
    transferredAtAfterOrEqual(
            Instant startDate
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(
                        root.get("transferredAt"),
                        startDate
                );
    }

    private static Specification<AccountTransfer>
    transferredAtBeforeOrEqual(
            Instant endDate
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(
                        root.get("transferredAt"),
                        endDate
                );
    }
}