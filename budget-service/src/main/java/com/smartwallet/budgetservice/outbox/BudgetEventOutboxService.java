package com.smartwallet.budgetservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwallet.budgetservice.entity.Budget;
import com.smartwallet.budgetservice.entity.BudgetStatus;
import com.smartwallet.budgetservice.entity.OutboxEvent;
import com.smartwallet.budgetservice.entity.OutboxEventStatus;
import com.smartwallet.budgetservice.repository.OutboxEventRepository;
import com.smartwallet.contracts.budget.BudgetExceededEvent;
import com.smartwallet.contracts.budget.BudgetMessagingConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetEventOutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueExceededIfNeeded(
            BudgetStatus previousStatus,
            Budget budget
    ) {
        boolean newlyExceeded =
                previousStatus != BudgetStatus.EXCEEDED
                        && budget.getStatus()
                        == BudgetStatus.EXCEEDED;

        if (!newlyExceeded) {
            return;
        }

        BudgetExceededEvent event =
                new BudgetExceededEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        budget.getId(),
                        budget.getUserId(),
                        budget.getCategoryId(),
                        budget.getLimitAmount(),
                        budget.getSpentAmount(),
                        budget.getYear(),
                        budget.getMonth()
                );

        enqueue(event);
    }

    private void enqueue(BudgetExceededEvent event) {
        try {
            String payload =
                    objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent =
                    OutboxEvent.builder()
                            .id(event.eventId())
                            .aggregateType("BUDGET")
                            .aggregateId(event.budgetId())
                            .eventType("BUDGET_EXCEEDED")
                            .routingKey(
                                    BudgetMessagingConstants
                                            .BUDGET_EXCEEDED_ROUTING_KEY
                            )
                            .payload(payload)
                            .status(OutboxEventStatus.PENDING)
                            .attemptCount(0)
                            .nextAttemptAt(Instant.now())
                            .createdAt(Instant.now())
                            .build();

            outboxEventRepository.save(outboxEvent);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Budget exceeded event could not be serialized",
                    exception
            );
        }
    }
}