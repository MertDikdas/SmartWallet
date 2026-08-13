package com.smartwallet.analyticsservice.repository;

import com.smartwallet.analyticsservice.dto.projection.CategoryExpenseAggregate;
import com.smartwallet.analyticsservice.dto.projection.MonthlyAggregate;
import com.smartwallet.analyticsservice.entity.CurrencyCode;
import com.smartwallet.analyticsservice.entity.ProjectionTransactionType;
import com.smartwallet.analyticsservice.entity.TransactionProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class TransactionProjectionRepositoryTest {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("analytics_test")
                    .withUsername("smartwallet")
                    .withPassword("smartwallet");

    @DynamicPropertySource
    static void configureDatabase(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );
    }

    @Autowired
    private TransactionProjectionRepository
            transactionProjectionRepository;

    @Test
    void shouldCalculateMonthlyAggregate() {
        transactionProjectionRepository.saveAll(
                List.of(
                        createProjection(
                                1L,
                                1L,
                                1L,
                                1L,
                                "Salary",
                                ProjectionTransactionType.INCOME,
                                "5000.00",
                                CurrencyCode.TRY,
                                "2026-07-05T10:00:00Z"
                        ),
                        createProjection(
                                2L,
                                1L,
                                1L,
                                2L,
                                "Food",
                                ProjectionTransactionType.EXPENSE,
                                "1200.00",
                                CurrencyCode.TRY,
                                "2026-07-10T10:00:00Z"
                        ),

                        // Başka ay: dahil edilmemeli
                        createProjection(
                                3L,
                                1L,
                                1L,
                                2L,
                                "Food",
                                ProjectionTransactionType.EXPENSE,
                                "300.00",
                                CurrencyCode.TRY,
                                "2026-08-02T10:00:00Z"
                        ),

                        // Başka kullanıcı: dahil edilmemeli
                        createProjection(
                                4L,
                                2L,
                                2L,
                                2L,
                                "Food",
                                ProjectionTransactionType.EXPENSE,
                                "999.00",
                                CurrencyCode.TRY,
                                "2026-07-15T10:00:00Z"
                        )
                )
        );

        MonthlyAggregate aggregate =
                transactionProjectionRepository
                        .calculateMonthlyAggregate(
                                1L,
                                Instant.parse(
                                        "2026-07-01T00:00:00Z"
                                ),
                                Instant.parse(
                                        "2026-08-01T00:00:00Z"
                                ),
                                ProjectionTransactionType.INCOME,
                                ProjectionTransactionType.EXPENSE
                        );

        assertThat(aggregate.totalIncome())
                .isEqualByComparingTo("5000.00");

        assertThat(aggregate.totalExpense())
                .isEqualByComparingTo("1200.00");

        assertThat(aggregate.transactionCount())
                .isEqualTo(2L);
    }

    @Test
    void shouldGroupMonthlyExpensesByCategory() {
        transactionProjectionRepository.saveAll(
                List.of(
                        createProjection(
                                10L,
                                1L,
                                1L,
                                1L,
                                "Food",
                                ProjectionTransactionType.EXPENSE,
                                "750.00",
                                CurrencyCode.TRY,
                                "2026-07-05T10:00:00Z"
                        ),
                        createProjection(
                                11L,
                                1L,
                                1L,
                                1L,
                                "Food",
                                ProjectionTransactionType.EXPENSE,
                                "250.00",
                                CurrencyCode.TRY,
                                "2026-07-06T10:00:00Z"
                        ),
                        createProjection(
                                12L,
                                1L,
                                1L,
                                2L,
                                "Travel",
                                ProjectionTransactionType.EXPENSE,
                                "500.00",
                                CurrencyCode.TRY,
                                "2026-07-07T10:00:00Z"
                        ),

                        // Income kategori giderine dahil edilmemeli
                        createProjection(
                                13L,
                                1L,
                                1L,
                                3L,
                                "Salary",
                                ProjectionTransactionType.INCOME,
                                "5000.00",
                                CurrencyCode.TRY,
                                "2026-07-08T10:00:00Z"
                        ),

                        // Başka kullanıcı dahil edilmemeli
                        createProjection(
                                14L,
                                2L,
                                2L,
                                1L,
                                "Food",
                                ProjectionTransactionType.EXPENSE,
                                "900.00",
                                CurrencyCode.TRY,
                                "2026-07-09T10:00:00Z"
                        )
                )
        );

        List<CategoryExpenseAggregate> result =
                transactionProjectionRepository
                        .calculateCategoryExpenses(
                                1L,
                                Instant.parse(
                                        "2026-07-01T00:00:00Z"
                                ),
                                Instant.parse(
                                        "2026-08-01T00:00:00Z"
                                ),
                                ProjectionTransactionType.EXPENSE
                        );

        assertThat(result)
                .hasSize(2);

        CategoryExpenseAggregate food =
                result.get(0);

        assertThat(food.categoryId())
                .isEqualTo(1L);

        assertThat(food.categoryName())
                .isEqualTo("Food");

        assertThat(food.totalExpense())
                .isEqualByComparingTo("1000.00");

        assertThat(food.transactionCount())
                .isEqualTo(2L);

        CategoryExpenseAggregate travel =
                result.get(1);

        assertThat(travel.categoryId())
                .isEqualTo(2L);

        assertThat(travel.categoryName())
                .isEqualTo("Travel");

        assertThat(travel.totalExpense())
                .isEqualByComparingTo("500.00");

        assertThat(travel.transactionCount())
                .isEqualTo(1L);
    }

    private TransactionProjection createProjection(
            Long transactionId,
            Long userId,
            Long accountId,
            Long categoryId,
            String categoryName,
            ProjectionTransactionType transactionType,
            String amount,
            CurrencyCode currency,
            String transactionDate
    ) {
        return TransactionProjection.builder()
                .transactionId(transactionId)
                .userId(userId)
                .accountId(accountId)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .transactionType(transactionType)
                .amount(new BigDecimal(amount))
                .currency(currency)
                .transactionDate(
                        Instant.parse(transactionDate)
                )
                .updatedAt(Instant.now())
                .build();
    }
}