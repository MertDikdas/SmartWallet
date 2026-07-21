package com.smartwallet.budgetservice.controller;

import com.smartwallet.budgetservice.dto.request.CreateBudgetRequest;
import com.smartwallet.budgetservice.dto.request.UpdateBudgetRequest;
import com.smartwallet.budgetservice.dto.response.BudgetResponse;
import com.smartwallet.budgetservice.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBudgetRequest request
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        BudgetResponse response =
                budgetService.createBudget(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                budgetService.getBudgets(userId)
        );
    }

    @GetMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> getBudget(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long budgetId
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                budgetService.getBudget(userId, budgetId)
        );
    }

    @PatchMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long budgetId,
            @Valid @RequestBody UpdateBudgetRequest request
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                budgetService.updateBudget(
                        userId,
                        budgetId,
                        request
                )
        );
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long budgetId
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        budgetService.deleteBudget(userId, budgetId);

        return ResponseEntity.noContent().build();
    }
}