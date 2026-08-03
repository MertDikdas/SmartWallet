package com.smartwallet.financeservice.controller;

import com.smartwallet.financeservice.dto.request.CreateRecurringTransactionRequest;
import com.smartwallet.financeservice.dto.response.RecurringTransactionExecutionResponse;
import com.smartwallet.financeservice.dto.response.RecurringTransactionResponse;
import com.smartwallet.financeservice.service.RecurringTransactionExecutionQueryService;
import com.smartwallet.financeservice.service.RecurringTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService
            recurringTransactionService;

    private final RecurringTransactionExecutionQueryService
            executionQueryService;

    @PostMapping
    public ResponseEntity<RecurringTransactionResponse>
    createRecurringTransaction(
            @AuthenticationPrincipal Jwt jwt,

            @Valid @RequestBody
            CreateRecurringTransactionRequest request
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        RecurringTransactionResponse response =
                recurringTransactionService
                        .createRecurringTransaction(
                                userId,
                                request
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<RecurringTransactionResponse>>
    getRecurringTransactions(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                recurringTransactionService
                        .getRecurringTransactions(
                                userId
                        )
        );
    }

    @GetMapping("/{recurringTransactionId}")
    public ResponseEntity<RecurringTransactionResponse>
    getRecurringTransaction(
            @AuthenticationPrincipal Jwt jwt,

            @PathVariable
            Long recurringTransactionId
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                recurringTransactionService
                        .getRecurringTransaction(
                                userId,
                                recurringTransactionId
                        )
        );
    }

    @PatchMapping("/{recurringTransactionId}/pause")
    public ResponseEntity<RecurringTransactionResponse>
    pauseRecurringTransaction(
            @AuthenticationPrincipal Jwt jwt,

            @PathVariable
            Long recurringTransactionId
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                recurringTransactionService
                        .pauseRecurringTransaction(
                                userId,
                                recurringTransactionId
                        )
        );
    }

    @PatchMapping("/{recurringTransactionId}/resume")
    public ResponseEntity<RecurringTransactionResponse>
    resumeRecurringTransaction(
            @AuthenticationPrincipal Jwt jwt,

            @PathVariable
            Long recurringTransactionId
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                recurringTransactionService
                        .resumeRecurringTransaction(
                                userId,
                                recurringTransactionId
                        )
        );
    }

    @DeleteMapping("/{recurringTransactionId}")
    public ResponseEntity<Void>
    cancelRecurringTransaction(
            @AuthenticationPrincipal Jwt jwt,

            @PathVariable
            Long recurringTransactionId
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        recurringTransactionService
                .cancelRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping("/{recurringTransactionId}/executions")
    public ResponseEntity<
            List<RecurringTransactionExecutionResponse>
            > getExecutionHistory(
            @AuthenticationPrincipal Jwt jwt,

            @PathVariable
            Long recurringTransactionId
    ) {
        Long userId =
                Long.valueOf(jwt.getSubject());

        List<RecurringTransactionExecutionResponse> response =
                executionQueryService.getExecutionHistory(
                        userId,
                        recurringTransactionId
                );

        return ResponseEntity.ok(response);
    }
}