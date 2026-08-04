package com.smartwallet.financeservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwallet.contracts.recurring.RecurringTransactionFailedEvent;
import com.smartwallet.contracts.recurring.RecurringTransactionMessagingConstants;
import com.smartwallet.contracts.transaction.TransactionChangedEvent;
import com.smartwallet.financeservice.entity.OutboxEvent;
import com.smartwallet.financeservice.entity.OutboxEventStatus;
import com.smartwallet.financeservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(TransactionChangedEvent event) {
        try {
            String payload =
                    objectMapper.writeValueAsString(event);

            Long aggregateId =
                    event.after() != null
                            ? event.after().transactionId()
                            : event.before().transactionId();

            OutboxEvent outboxEvent =
                    OutboxEvent.builder()
                            .id(event.eventId())
                            .aggregateType("TRANSACTION")
                            .aggregateId(aggregateId)
                            .eventType(event.eventType().name())
                            .routingKey(event.routingKey())
                            .payload(payload)
                            .status(OutboxEventStatus.PENDING)
                            .attemptCount(0)
                            .nextAttemptAt(Instant.now())
                            .createdAt(Instant.now())
                            .build();

            outboxEventRepository.save(outboxEvent);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Transaction event could not be serialized",
                    exception
            );
        }
    }

    @Transactional(
            propagation = Propagation.MANDATORY
    )
    public void enqueue(
            RecurringTransactionFailedEvent event
    ) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            Instant now = Instant.now();

            OutboxEvent outboxEvent =
                    OutboxEvent.builder()
                            .id(event.eventId())
                            .aggregateType("RECURRING_TRANSACTION")
                            .aggregateId(event.recurringTransactionId())
                            .eventType("RECURRING_TRANSACTION_FAILED")
                            .routingKey(RecurringTransactionMessagingConstants.FAILED_ROUTING_KEY)
                            .payload(payload)
                            .status(OutboxEventStatus.PENDING)
                            .attemptCount(0)
                            .nextAttemptAt(now)
                            .createdAt(now)
                            .build();
            outboxEventRepository.save(outboxEvent);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Transaction event could not be serialized",
                    exception
            );
        }
    }
}