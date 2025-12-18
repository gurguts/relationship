package org.example.purchaseservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.ExchangeRate;
import org.example.purchaseservice.repositories.ExchangeRateRepository;
import org.example.purchaseservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    
    private final ExchangeRateRepository exchangeRateRepository;
    
    /**
     * Get exchange rate for currency to EUR
     * @param fromCurrency UAH or USD
     * @return exchange rate
     */
    @Transactional(readOnly = true)
    public BigDecimal getExchangeRateToEur(String fromCurrency) {
        if (fromCurrency == null || "EUR".equalsIgnoreCase(fromCurrency)) {
            return BigDecimal.ONE;
        }
        
        ExchangeRate rate = exchangeRateRepository.findByFromCurrency(fromCurrency.toUpperCase())
                .orElseThrow(() -> new PurchaseException("EXCHANGE_RATE_NOT_FOUND", 
                        String.format("Курс валют для %s не знайдено. Будь ласка, встановіть курс в налаштуваннях.", fromCurrency)));
        
        return rate.getRate();
    }
    
    /**
     * Get all exchange rates
     */
    @Transactional(readOnly = true)
    public List<ExchangeRate> getAllExchangeRates() {
        return exchangeRateRepository.findAll();
    }
    
    /**
     * Update exchange rate
     */
    @Transactional
    public ExchangeRate updateExchangeRate(String fromCurrency, BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_RATE", "Курс валют повинен бути більше нуля");
        }
        
        Long userId = SecurityUtils.getCurrentUserId();
        
        ExchangeRate exchangeRate = exchangeRateRepository.findByFromCurrency(fromCurrency.toUpperCase())
                .orElseGet(() -> {
                    ExchangeRate newRate = new ExchangeRate();
                    newRate.setFromCurrency(fromCurrency.toUpperCase());
                    newRate.setToCurrency("EUR");
                    return newRate;
                });
        
        exchangeRate.setRate(rate);
        exchangeRate.setUpdatedByUserId(userId);
        
        return exchangeRateRepository.save(exchangeRate);
    }
    
    /**
     * Get exchange rate entity
     */
    @Transactional(readOnly = true)
    public ExchangeRate getExchangeRate(String fromCurrency) {
        return exchangeRateRepository.findByFromCurrency(fromCurrency.toUpperCase())
                .orElse(null);
    }
}


