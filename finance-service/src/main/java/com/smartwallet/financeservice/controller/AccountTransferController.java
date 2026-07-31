package com.smartwallet.financeservice.controller;

import com.smartwallet.financeservice.dto.request.CreateTransferRequest;
import com.smartwallet.financeservice.dto.request.TransferFilterRequest;
import com.smartwallet.financeservice.dto.response.PageResponse;
import com.smartwallet.financeservice.dto.response.TransferResponse;
import com.smartwallet.financeservice.service.AccountTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class AccountTransferController {

    private final AccountTransferService accountTransferService;

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        TransferResponse response = accountTransferService.createTransfer(
                userId,
                idempotencyKey,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<TransferResponse>> getTransfers(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute TransferFilterRequest filter
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                accountTransferService.getTransfers(
                        userId,
                        filter
                )
        );
    }

}
