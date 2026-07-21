package com.smartwallet.financeservice.controller;

import com.smartwallet.financeservice.dto.request.CreateAccountRequest;
import com.smartwallet.financeservice.dto.request.UpdateAccountRequest;
import com.smartwallet.financeservice.dto.response.AccountResponse;
import com.smartwallet.financeservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAccountRequest request
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        AccountResponse response =
                accountService.createAccount(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                accountService.getAccounts(userId)
        );
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long accountId
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                accountService.getAccount(userId, accountId)
        );
    }

    @PatchMapping("/{accountId}")
    public ResponseEntity<AccountResponse> updateAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                accountService.updateAccount(
                        userId,
                        accountId,
                        request
                )
        );
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long accountId
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        accountService.deleteAccount(userId, accountId);

        return ResponseEntity.noContent().build();
    }
}