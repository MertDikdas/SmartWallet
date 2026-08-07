package com.smartwallet.budgetservice.messaging;

import com.smartwallet.budgetservice.entity.Budget;
import com.smartwallet.budgetservice.entity.MonthlyCategorySpending;
import com.smartwallet.budgetservice.outbox.BudgetEventOutboxService;
import com.smartwallet.budgetservice.entity.BudgetStatus;
import com.smartwallet.budgetservice.repository.BudgetRepository;
import com.smartwallet.budgetservice.repository.MonthlyCategorySpendingRepository;
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
    private final BudgetEventOutboxService budgetEventOutboxService;

    private final ProcessedTransactionEventRepository
            processedEventRepository;

    private final MonthlyCategorySpendingRepository monthlyCategorySpendingRepository;

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

        if (!"EXPENSE".equals(snapshot.transactionType())) {
            return;
        }

        YearMonth period = YearMonth.from(
                snapshot.transactionDate()
                        .atZone(ZoneOffset.UTC)
        );

        Budget budget = budgetRepository
                .findForUpdate(
                        snapshot.userId(),
                        snapshot.categoryId(),
                        period.getYear(),
                        period.getMonthValue()
                )
                .orElse(null);

        if (budget != null) {
            updateSpentAmount(
                    budget,
                    snapshot.amount(),
                    multiplier
            );
        }
        MonthlyCategorySpending spending = monthlyCategorySpendingRepository
                .findByUserIdAndCategoryIdAndYearAndMonth(
                        snapshot.userId(),
                        snapshot.categoryId(),
                        period.getYear(),
                        period.getMonthValue()
                )
                .orElse(null);

        if (spending != null) {
            addMonthlySpending(
                    spending,
                    snapshot.amount(),
                    multiplier
            );
        }else {
            MonthlyCategorySpending newSpending = MonthlyCategorySpending.builder()
                    .categoryId(snapshot.categoryId())
                    .userId(snapshot.userId())
                    .spentAmount(snapshot.amount())
                    .year(period.getYear())
                    .month(period.getMonthValue())
                    .build();
            monthlyCategorySpendingRepository.save(newSpending);
        }
    }

    private void addMonthlySpending(
            MonthlyCategorySpending monthlyCategorySpending,
            BigDecimal transactionAmount,
            BigDecimal multiplier
    ){

        BigDecimal difference =
                transactionAmount.multiply(multiplier);
        BigDecimal newSpentAmount =
                monthlyCategorySpending.getSpentAmount().add(difference);

        if (newSpentAmount.signum() < 0) {
            newSpentAmount = BigDecimal.ZERO;
        }

        monthlyCategorySpending.setSpentAmount(newSpentAmount);
    }

    private void updateSpentAmount(
            Budget budget,
            BigDecimal transactionAmount,
            BigDecimal multiplier
    ) {
        BudgetStatus previousStatus =
                budget.getStatus();

        BigDecimal difference =
                transactionAmount.multiply(multiplier);

        BigDecimal newSpentAmount =
                budget.getSpentAmount().add(difference);

        if (newSpentAmount.signum() < 0) {
            newSpentAmount = BigDecimal.ZERO;
        }

        budget.setSpentAmount(newSpentAmount);
        budget.recalculateStatus();

        budgetEventOutboxService.enqueueExceededIfNeeded(
                previousStatus,
                budget
        );
    }
}