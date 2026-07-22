package com.smartwallet.budgetservice.messaging;

import com.smartwallet.budgetservice.entity.Budget;
import com.smartwallet.budgetservice.repository.BudgetRepository;
import com.smartwallet.budgetservice.repository.ProcessedTransactionEventRepository;
import com.smartwallet.contracts.transaction.TransactionChangedEvent;
import com.smartwallet.contracts.transaction.TransactionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class BudgetTransactionEventHandler {

    private final BudgetRepository budgetRepository;

    private final ProcessedTransactionEventRepository
            processedEventRepository;

    @Transactional
    public void handle(TransactionChangedEvent event) {

        int inserted =
                processedEventRepository.insertIfAbsent(
                        event.eventId(),
                        event.eventType().name()
                );

        // Event daha önce işlendi
        if (inserted == 0) {
            return;
        }

        switch (event.eventType()) {
            case CREATED ->
                    applySnapshot(
                            event.after(),
                            BigDecimal.ONE
                    );

            case UPDATED -> {
                // Eski işlemin etkisini kaldır
                applySnapshot(
                        event.before(),
                        BigDecimal.ONE.negate()
                );

                // Yeni işlemin etkisini uygula
                applySnapshot(
                        event.after(),
                        BigDecimal.ONE
                );
            }

            case DELETED ->
                    applySnapshot(
                            event.before(),
                            BigDecimal.ONE.negate()
                    );
        }
    }

    private void applySnapshot(
            TransactionSnapshot snapshot,
            BigDecimal multiplier
    ) {
        if (snapshot == null) {
            return;
        }

        // Gelirler bütçe harcamasını değiştirmez
        if (!"EXPENSE".equals(snapshot.transactionType())) {
            return;
        }

        YearMonth period = YearMonth.from(
                snapshot.transactionDate()
                        .atZone(ZoneOffset.UTC)
        );

        budgetRepository
                .findForUpdate(
                        snapshot.userId(),
                        snapshot.categoryId(),
                        period.getYear(),
                        period.getMonthValue()
                )
                .ifPresent(budget ->
                        updateSpentAmount(
                                budget,
                                snapshot.amount(),
                                multiplier
                        )
                );
    }

    private void updateSpentAmount(
            Budget budget,
            BigDecimal transactionAmount,
            BigDecimal multiplier
    ) {
        BigDecimal difference =
                transactionAmount.multiply(multiplier);

        BigDecimal newSpentAmount =
                budget.getSpentAmount().add(difference);

        // Eski event'leri olmayan transaction update/delete
        // işlemlerinde negatif değeri engeller.
        if (newSpentAmount.signum() < 0) {
            newSpentAmount = BigDecimal.ZERO;
        }

        budget.setSpentAmount(newSpentAmount);
        budget.recalculateStatus();
    }
}