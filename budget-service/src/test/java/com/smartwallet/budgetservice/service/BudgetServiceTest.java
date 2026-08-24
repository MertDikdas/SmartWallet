package com.smartwallet.budgetservice.service;


import com.smartwallet.budgetservice.client.CategoryClient;
import com.smartwallet.budgetservice.client.dto.CategoryDetailsResponse;
import com.smartwallet.budgetservice.dto.request.CreateBudgetRequest;
import com.smartwallet.budgetservice.dto.response.BudgetResponse;
import com.smartwallet.budgetservice.entity.Budget;
import com.smartwallet.budgetservice.entity.CurrencyCode;
import com.smartwallet.budgetservice.exception.InvalidBudgetCategoryException;
import com.smartwallet.budgetservice.mapper.BudgetMapper;
import com.smartwallet.budgetservice.outbox.BudgetEventOutboxService;
import com.smartwallet.budgetservice.repository.BudgetRepository;
import com.smartwallet.budgetservice.repository.MonthlyCategorySpendingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private BudgetMapper budgetMapper;
    @Mock
    private BudgetEventOutboxService budgetEventOutboxService;
    @Mock
    private CategoryClient categoryClient;

    @Mock
    private MonthlyCategorySpendingRepository spendingRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void shouldRejectCategoryBelongingToAnotherUser() {
        Long userId = 1L;
        String accessToken = "access-token";

        CreateBudgetRequest request =
                new CreateBudgetRequest(
                        99L,
                        new BigDecimal("1000.00"),
                        2026,
                        7,
                        CurrencyCode.TRY
                );

        when(
                categoryClient.getOwnedCategory(
                        99L,
                        accessToken
                )
        ).thenThrow(
                new InvalidBudgetCategoryException(99L)
        );

        assertThatThrownBy(
                () -> budgetService.createBudget(
                        userId,
                        accessToken,
                        request
                )
        ).isInstanceOf(
                InvalidBudgetCategoryException.class
        );

        verify(
                budgetRepository,
                never()
        ).save(any(Budget.class));
    }

    @Test
    void shouldCreateBudget() {
        // Arrange
        Long userId = 1L;
        String accessToken = "access-token";

        CreateBudgetRequest request =
                new CreateBudgetRequest(
                        99L,
                        new BigDecimal("1000.00"),
                        2026,
                        7,
                        CurrencyCode.TRY
                );

        CategoryDetailsResponse category =
                new CategoryDetailsResponse(
                        99L,
                        "FOOD",
                        "EXPENSE",
                        Instant.parse("2026-07-28T10:00:00Z")
                );

        BudgetResponse expectedResponse =
                mock(BudgetResponse.class);

        when(expectedResponse.month())
                .thenReturn(7);

        when(expectedResponse.categoryId())
                .thenReturn(99L);

        when(expectedResponse.year())
                .thenReturn(2026);

        when(expectedResponse.limitAmount())
                .thenReturn(new BigDecimal("1000.00"));

        when(
                categoryClient.getOwnedCategory(
                        99L,
                        accessToken
                )
        ).thenReturn(category);

        when(
                budgetRepository.save(any(Budget.class))
        ).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        when(
                budgetMapper.toResponse(any(Budget.class))
        ).thenReturn(expectedResponse);

        // Act
        BudgetResponse response =
                budgetService.createBudget(
                        userId,
                        accessToken,
                        request
                );

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.month()).isEqualTo(7);
        assertThat(response.categoryId()).isEqualTo(99L);
        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.limitAmount())
                .isEqualByComparingTo("1000.00");

        verify(categoryClient)
                .getOwnedCategory(
                        99L,
                        accessToken
                );

        verify(budgetRepository)
                .save(any(Budget.class));

        verify(budgetMapper)
                .toResponse(any(Budget.class));
    }
}
