package com.smartwallet.financeservice.client;

import com.smartwallet.financeservice.exception.BudgetFoundForCategoryException;
import com.smartwallet.financeservice.exception.InvalidBudgetCategoryException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class BudgetClient {

    private final RestClient budgetRestClient;

    public Boolean getCategoryBudget(
            Long categoryId,
            String accessToken
    ){
        try {
            Boolean isExists =
                    budgetRestClient
                            .get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/api/budgets/categories/{categoryId}")
                                    .build(categoryId)
                            )
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + accessToken
                            )
                            .retrieve()
                            .body(new ParameterizedTypeReference<Boolean>() {});

            if (isExists) {
                throw new BudgetFoundForCategoryException();
            }

            return isExists;

        } catch (
                HttpClientErrorException.NotFound |
                HttpClientErrorException.Forbidden |
                HttpClientErrorException.Unauthorized exception
        ) {
            throw new InvalidBudgetCategoryException(categoryId);
        }
    }
}