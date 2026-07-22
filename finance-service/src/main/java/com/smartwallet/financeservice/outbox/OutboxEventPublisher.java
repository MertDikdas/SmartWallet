package com.smartwallet.financeservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwallet.contracts.transaction.TransactionChangedEvent;
import com.smartwallet.contracts.transaction.TransactionMessagingConstants;
import com.smartwallet.financeservice.entity.OutboxEvent;
import com.smartwallet.financeservice.entity.OutboxEventStatus;
import com.smartwallet.financeservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(
            fixedDelayString =
                    "${outbox.publisher.fixed-delay-ms:2000}"
    )
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events =
                outboxEventRepository
                        .findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                                OutboxEventStatus.PENDING,
                                Instant.now(),
                                PageRequest.of(0, BATCH_SIZE)
                        );

        for (OutboxEvent outboxEvent : events) {
            publishEvent(outboxEvent);
        }
    }

    private void publishEvent(OutboxEvent outboxEvent) {
        try {
            TransactionChangedEvent event =
                    objectMapper.readValue(
                            outboxEvent.getPayload(),
                            TransactionChangedEvent.class
                    );

            rabbitTemplate.convertAndSend(
                    TransactionMessagingConstants.EXCHANGE,
                    outboxEvent.getRoutingKey(),
                    event
            );

            outboxEvent.markPublished();

            log.info(
                    "Outbox event published. eventId={}, type={}",
                    outboxEvent.getId(),
                    outboxEvent.getEventType()
            );

        } catch (Exception exception) {
            outboxEvent.markFailed(exception);

            log.error(
                    "Outbox event publishing failed. eventId={}, attempt={}",
                    outboxEvent.getId(),
                    outboxEvent.getAttemptCount(),
                    exception
            );
        }
    }
}