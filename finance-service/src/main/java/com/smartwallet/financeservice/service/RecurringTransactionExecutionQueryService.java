package com.smartwallet.financeservice.service;

import com.smartwallet.financeservice.dto.response.RecurringTransactionExecutionResponse;
import com.smartwallet.financeservice.exception.RecurringTransactionNotFoundException;
import com.smartwallet.financeservice.mapper.RecurringTransactionExecutionMapper;
import com.smartwallet.financeservice.repository.RecurringTransactionExecutionRepository;
import com.smartwallet.financeservice.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringTransactionExecutionQueryService {

    private final RecurringTransactionRepository
            recurringTransactionRepository;

    private final RecurringTransactionExecutionRepository
            executionRepository;

    private final RecurringTransactionExecutionMapper
            executionMapper;

    @Transactional(readOnly = true)
    public List<RecurringTransactionExecutionResponse>
    getExecutionHistory(
            Long userId,
            Long recurringTransactionId
    ) {

        recurringTransactionRepository
                .findByIdAndUserId(
                        recurringTransactionId,
                        userId
                )
                .orElseThrow(
                        RecurringTransactionNotFoundException::new
                );

        return executionRepository
                .findAllByRecurringTransactionIdOrderByScheduledDateDesc(
                        recurringTransactionId
                )
                .stream()
                .map(executionMapper::toResponse)
                .toList();
    }
}