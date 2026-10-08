package com.ffenf.app.repo;

import com.ffenf.app.domain.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {
    java.util.List<CoinTransaction> findByUserId(UUID userId);
}
