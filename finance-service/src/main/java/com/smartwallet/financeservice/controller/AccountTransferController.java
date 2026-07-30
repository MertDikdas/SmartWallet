package com.smartwallet.financeservice.controller;

import com.smartwallet.financeservice.dto.request.CreateTransferRequest;
import com.smartwallet.financeservice.dto.response.TransferResponse;
import com.smartwallet.financeservice.service.AccountTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class AccountTransferController {

    private final AccountTransferService accountTransferService;

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        Long userId =
                Long.parseLong(jwt.getSubject());

        TransferResponse response = accountTransferService.createTransfer(
                userId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
