package com.smartwallet.financeservice.repository;

import com.smartwallet.financeservice.entity.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, Long> {

    List<FinancialTransaction>
    findAllByUserIdOrderByTransactionDateDesc(Long userId);
}