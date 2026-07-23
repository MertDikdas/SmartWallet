package com.smartwallet.analyticsservice.messaging;

import com.smartwallet.analyticsservice.entity.TransactionProjection;
import com.smartwallet.analyticsservice.repository.ProcessedTransactionEventRepository;
import com.smartwallet.analyticsservice.repository.TransactionProjectionRepository;
import com.smartwallet.contracts.transaction.TransactionChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionEventHandler {

    private final TransactionProjectionRepository
            projectionRepository;

    private final ProcessedTransactionEventRepository
            processedEventRepository;

    @Transactional
    public void handle(TransactionChangedEvent event) {

        int inserted =
                processedEventRepository.insertIfAbsent(
                        event.eventId(),
                        event.eventType().name()
                );

        if (inserted == 0) {
            return;
        }

        switch (event.eventType()) {
            case CREATED, UPDATED ->
                    projectionRepository.save(
                            TransactionProjection.from(
                                    event.after()
                            )
                    );

            case DELETED ->
                    projectionRepository.deleteById(
                            event.before().transactionId()
                    );
        }
    }
}