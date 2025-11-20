package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByFromCurrency(String fromCurrency);
}


