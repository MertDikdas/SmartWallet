package com.smartwallet.analyticsservice.messaging;

import com.smartwallet.analyticsservice.entity.ProjectionTransactionType;
import com.smartwallet.analyticsservice.entity.TransactionProjection;
import com.smartwallet.analyticsservice.repository.ProcessedTransactionEventRepository;
import com.smartwallet.analyticsservice.repository.TransactionProjectionRepository;
import com.smartwallet.contracts.transaction.CurrencyCode;
import com.smartwallet.contracts.transaction.TransactionChangedEvent;
import com.smartwallet.contracts.transaction.TransactionEventType;
import com.smartwallet.contracts.transaction.TransactionSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionEventHandlerTest {

    @Mock
    private TransactionProjectionRepository
            transactionProjectionRepository;

    @Mock
    private ProcessedTransactionEventRepository
            processedTransactionEventRepository;

    @InjectMocks
    private TransactionEventHandler
            transactionEventHandler;

    @Test
    void shouldCreateProjectionForCreatedEvent() {
        UUID eventId = UUID.randomUUID();

        TransactionSnapshot after =
                createSnapshot(
                        10L,
                        1L,
                        2L,
                        3L,
                        "Food",
                        "EXPENSE",
                        CurrencyCode.TRY,
                        "250.00"
                );

        TransactionChangedEvent event =
                new TransactionChangedEvent(
                        eventId,
                        TransactionEventType.CREATED,
                        Instant.parse("2026-07-24T12:00:00Z"),
                        null,
                        after
                );

        when(
                processedTransactionEventRepository
                        .insertIfAbsent(
                                eventId,
                                "CREATED"
                        )
        ).thenReturn(1);

        transactionEventHandler.handle(event);

        ArgumentCaptor<TransactionProjection> captor =
                ArgumentCaptor.forClass(
                        TransactionProjection.class
                );

        verify(transactionProjectionRepository)
                .save(captor.capture());

        TransactionProjection savedProjection =
                captor.getValue();

        assertThat(savedProjection.getTransactionId())
                .isEqualTo(10L);

        assertThat(savedProjection.getUserId())
                .isEqualTo(1L);

        assertThat(savedProjection.getAccountId())
                .isEqualTo(2L);

        assertThat(savedProjection.getCategoryId())
                .isEqualTo(3L);

        assertThat(savedProjection.getCategoryName())
                .isEqualTo("Food");

        assertThat(savedProjection.getTransactionType())
                .isEqualTo(
                        ProjectionTransactionType.EXPENSE
                );

        assertThat(savedProjection.getAmount())
                .isEqualByComparingTo("250.00");

        verify(
                transactionProjectionRepository,
                never()
        ).deleteById(10L);
    }

    @Test
    void shouldUpdateProjectionUsingAfterSnapshot() {
        UUID eventId = UUID.randomUUID();

        TransactionSnapshot before =
                createSnapshot(
                        10L,
                        1L,
                        2L,
                        3L,
                        "Food",
                        "EXPENSE",
                        CurrencyCode.TRY,
                        "250.00"
                );

        TransactionSnapshot after =
                createSnapshot(
                        10L,
                        1L,
                        2L,
                        4L,
                        "Travel",
                        "EXPENSE",
                        CurrencyCode.TRY,
                        "500.00"
                );

        TransactionChangedEvent event =
                new TransactionChangedEvent(
                        eventId,
                        TransactionEventType.UPDATED,
                        Instant.parse("2026-07-24T12:00:00Z"),
                        before,
                        after
                );

        when(
                processedTransactionEventRepository
                        .insertIfAbsent(
                                eventId,
                                "UPDATED"
                        )
        ).thenReturn(1);

        transactionEventHandler.handle(event);

        ArgumentCaptor<TransactionProjection> captor =
                ArgumentCaptor.forClass(
                        TransactionProjection.class
                );

        verify(transactionProjectionRepository)
                .save(captor.capture());

        TransactionProjection savedProjection =
                captor.getValue();

        assertThat(savedProjection.getTransactionId())
                .isEqualTo(10L);

        assertThat(savedProjection.getCategoryId())
                .isEqualTo(4L);

        assertThat(savedProjection.getCategoryName())
                .isEqualTo("Travel");

        assertThat(savedProjection.getAmount())
                .isEqualByComparingTo("500.00");
    }

    @Test
    void shouldDeleteProjectionForDeletedEvent() {
        UUID eventId = UUID.randomUUID();

        TransactionSnapshot before =
                createSnapshot(
                        10L,
                        1L,
                        2L,
                        3L,
                        "Food",
                        "EXPENSE",
                        CurrencyCode.TRY,
                        "250.00"
                );

        TransactionChangedEvent event =
                new TransactionChangedEvent(
                        eventId,
                        TransactionEventType.DELETED,
                        Instant.parse("2026-07-24T12:00:00Z"),
                        before,
                        null
                );

        when(
                processedTransactionEventRepository
                        .insertIfAbsent(
                                eventId,
                                "DELETED"
                        )
        ).thenReturn(1);

        transactionEventHandler.handle(event);

        verify(transactionProjectionRepository)
                .deleteById(10L);

        verify(
                transactionProjectionRepository,
                never()
        ).save(
                org.mockito.ArgumentMatchers
                        .any(TransactionProjection.class)
        );
    }

    @Test
    void shouldIgnoreAlreadyProcessedEvent() {
        UUID eventId = UUID.randomUUID();

        TransactionSnapshot after =
                createSnapshot(
                        10L,
                        1L,
                        2L,
                        3L,
                        "Food",
                        "EXPENSE",
                        CurrencyCode.TRY,
                        "250.00"
                );

        TransactionChangedEvent event =
                new TransactionChangedEvent(
                        eventId,
                        TransactionEventType.CREATED,
                        Instant.parse("2026-07-24T12:00:00Z"),
                        null,
                        after
                );

        when(
                processedTransactionEventRepository
                        .insertIfAbsent(
                                eventId,
                                "CREATED"
                        )
        ).thenReturn(0);

        transactionEventHandler.handle(event);

        verifyNoInteractions(
                transactionProjectionRepository
        );
    }

    private TransactionSnapshot createSnapshot(
            Long transactionId,
            Long userId,
            Long accountId,
            Long categoryId,
            String categoryName,
            String transactionType,
            CurrencyCode currency,
            String amount
    ) {
        return new TransactionSnapshot(
                transactionId,
                userId,
                accountId,
                categoryId,
                categoryName,
                transactionType,
                new BigDecimal(amount),
                currency,
                Instant.parse("2026-07-24T10:00:00Z")
        );
    }
}