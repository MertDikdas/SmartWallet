package com.smartwallet.budgetservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwallet.budgetservice.entity.OutboxEvent;
import com.smartwallet.budgetservice.entity.OutboxEventStatus;
import com.smartwallet.budgetservice.repository.OutboxEventRepository;
import com.smartwallet.contracts.budget.BudgetExceededEvent;
import com.smartwallet.contracts.budget.BudgetMessagingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final int BATCH_SIZE = 50;
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

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
            BudgetExceededEvent event =
                    objectMapper.readValue(
                            outboxEvent.getPayload(),
                            BudgetExceededEvent.class
                    );

            CorrelationData correlationData =
                    new CorrelationData(
                            outboxEvent.getId().toString()
                    );

            rabbitTemplate.convertAndSend(
                    BudgetMessagingConstants.EXCHANGE,
                    outboxEvent.getRoutingKey(),
                    event,
                    message -> {
                        message.getMessageProperties()
                                .setDeliveryMode(
                                        MessageDeliveryMode.PERSISTENT
                                );

                        message.getMessageProperties()
                                .setMessageId(
                                        outboxEvent.getId().toString()
                                );

                        return message;
                    },
                    correlationData
            );

            CorrelationData.Confirm confirm =
                    correlationData
                            .getFuture()
                            .get(
                                    CONFIRM_TIMEOUT_SECONDS,
                                    TimeUnit.SECONDS
                            );

            ReturnedMessage returnedMessage =
                    correlationData.getReturned();

            if (returnedMessage != null) {
                throw new IllegalStateException(
                        "RabbitMQ returned budget event: "
                                + returnedMessage.getReplyText()
                );
            }

            if (!confirm.isAck()) {
                throw new IllegalStateException(
                        "RabbitMQ NACK: "
                                + confirm.getReason()
                );
            }

            outboxEvent.markPublished();

            log.info(
                    "Budget outbox event published. eventId={}, type={}",
                    outboxEvent.getId(),
                    outboxEvent.getEventType()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            markFailure(outboxEvent, exception);

        } catch (Exception exception) {
            markFailure(outboxEvent, exception);
        }
    }

    private void markFailure(
            OutboxEvent outboxEvent,
            Exception exception
    ) {
        outboxEvent.markFailed(exception);

        log.error(
                "Budget outbox publishing failed. eventId={}, attempt={}",
                outboxEvent.getId(),
                outboxEvent.getAttemptCount(),
                exception
        );
    }
}