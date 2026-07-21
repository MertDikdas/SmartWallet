package com.smartwallet.financeservice.repository;

import com.smartwallet.financeservice.entity.Category;
import com.smartwallet.financeservice.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

    List<Category> findAllByUserIdOrderByNameAsc(Long userId);

    Optional<Category> findByIdAndUserId(
            Long categoryId,
            Long userId
    );

    boolean existsByUserIdAndNameIgnoreCaseAndType(
            Long userId,
            String name,
            TransactionType type
    );
}