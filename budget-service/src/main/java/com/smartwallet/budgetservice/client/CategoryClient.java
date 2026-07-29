package com.smartwallet.budgetservice.client;

import com.smartwallet.budgetservice.client.dto.CategoryDetailsResponse;
import com.smartwallet.budgetservice.exception.InvalidBudgetCategoryException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CategoryClient {

    private final RestClient financeRestClient;

    public CategoryDetailsResponse getOwnedCategory(
            Long categoryId,
            String accessToken
    ) {
        try {
            CategoryDetailsResponse category =
                    financeRestClient
                            .get()
                            .uri(
                                    "/api/categories/{categoryId}",
                                    categoryId
                            )
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + accessToken
                            )
                            .retrieve()
                            .body(CategoryDetailsResponse.class);

            if (category == null) {
                throw new InvalidBudgetCategoryException(
                        categoryId
                );
            }

            return category;

        } catch (
                HttpClientErrorException.NotFound |
                HttpClientErrorException.Forbidden |
                HttpClientErrorException.Unauthorized exception
        ) {
            throw new InvalidBudgetCategoryException(
                    categoryId
            );
        }
    }
}
