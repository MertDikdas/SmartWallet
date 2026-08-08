package com.smartwallet.financeservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwallet.contracts.recurring.RecurringTransactionFailedEvent;
import com.smartwallet.contracts.recurring.RecurringTransactionMessagingConstants;
import com.smartwallet.contracts.transaction.TransactionChangedEvent;
import com.smartwallet.contracts.transaction.TransactionMessagingConstants;
import com.smartwallet.financeservice.entity.OutboxEvent;
import com.smartwallet.financeservice.entity.OutboxEventStatus;
import com.smartwallet.financeservice.repository.OutboxEventRepository;
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

    private void publishEvent(
            OutboxEvent outboxEvent
    ) {
        try {
            Object event;
            String exchange;

            switch (outboxEvent.getAggregateType()) {

                case "TRANSACTION" -> {
                    event =
                            objectMapper.readValue(
                                    outboxEvent.getPayload(),
                                    TransactionChangedEvent.class
                            );

                    exchange =
                            TransactionMessagingConstants.EXCHANGE;
                }

                case "RECURRING_TRANSACTION" -> {
                    event =
                            objectMapper.readValue(
                                    outboxEvent.getPayload(),
                                    RecurringTransactionFailedEvent.class
                            );

                    exchange =
                            RecurringTransactionMessagingConstants.EXCHANGE;
                }

                default -> throw new IllegalStateException(
                        "Unsupported outbox aggregate type: "
                                + outboxEvent.getAggregateType()
                );
            }

            CorrelationData correlationData =
                    new CorrelationData(
                            outboxEvent.getId().toString()
                    );

            rabbitTemplate.convertAndSend(
                    exchange,
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
                        "RabbitMQ returned the message. "
                                + "replyCode="
                                + returnedMessage.getReplyCode()
                                + ", replyText="
                                + returnedMessage.getReplyText()
                                + ", exchange="
                                + returnedMessage.getExchange()
                                + ", routingKey="
                                + returnedMessage.getRoutingKey()
                );
            }

            if (!confirm.ack()) {
                throw new IllegalStateException(
                        "RabbitMQ negatively acknowledged the message. "
                                + "reason="
                                + confirm.reason()
                );
            }

            outboxEvent.markPublished();

            log.info(
                    "Outbox event confirmed and published. "
                            + "eventId={}, eventType={}, routingKey={}",
                    outboxEvent.getId(),
                    outboxEvent.getEventType(),
                    outboxEvent.getRoutingKey()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            markPublishingFailure(
                    outboxEvent,
                    exception
            );

        } catch (Exception exception) {
            markPublishingFailure(
                    outboxEvent,
                    exception
            );
        }
    }

    private void markPublishingFailure(
            OutboxEvent outboxEvent,
            Exception exception
    ) {
        outboxEvent.markFailed(exception);

        log.error(
                "Outbox event publishing failed. "
                        + "eventId={}, attempt={}, status={}, nextAttemptAt={}",
                outboxEvent.getId(),
                outboxEvent.getAttemptCount(),
                outboxEvent.getStatus(),
                outboxEvent.getNextAttemptAt(),
                exception
        );
    }
}