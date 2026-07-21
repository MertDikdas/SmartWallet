package com.smartwallet.financeservice.controller;

import com.smartwallet.financeservice.dto.request.CreateTransactionRequest;
import com.smartwallet.financeservice.dto.response.TransactionResponse;
import com.smartwallet.financeservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        TransactionResponse response =
                transactionService.createTransaction(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                transactionService.getTransactions(userId)
        );
    }
}