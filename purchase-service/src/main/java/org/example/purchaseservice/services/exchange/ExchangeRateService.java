package org.example.purchaseservice.services.exchange;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.ExchangeRate;
import org.example.purchaseservice.repositories.ExchangeRateRepository;
import org.example.purchaseservice.utils.SecurityUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService implements IExchangeRateService {
    
    private static final String EUR_CURRENCY = "EUR";
    
    private final ExchangeRateRepository exchangeRateRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "exchangeRates", key = "#fromCurrency.toUpperCase()")
    public BigDecimal getExchangeRateToEur(@NonNull String fromCurrency) {
        String normalizedCurrency = normalizeCurrency(fromCurrency);
        
        if (EUR_CURRENCY.equalsIgnoreCase(normalizedCurrency)) {
            return BigDecimal.ONE;
        }
        
        ExchangeRate rate = exchangeRateRepository.findByFromCurrency(normalizedCurrency)
                .orElseThrow(() -> new PurchaseException("EXCHANGE_RATE_NOT_FOUND", 
                        String.format("Exchange rate for %s not found. Please set the rate in settings.", normalizedCurrency)));
        
        BigDecimal exchangeRate = rate.getRate();
        if (exchangeRate == null) {
            throw new PurchaseException("INVALID_EXCHANGE_RATE", 
                    String.format("Exchange rate value is null for currency: %s", normalizedCurrency));
        }
        
        return exchangeRate;
    }

    @Transactional(readOnly = true)
    public List<ExchangeRate> getAllExchangeRates() {
        return exchangeRateRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "exchangeRates", key = "#fromCurrency.toUpperCase()")
    public ExchangeRate updateExchangeRate(@NonNull String fromCurrency, @NonNull BigDecimal rate) {
        String normalizedCurrency = normalizeCurrency(fromCurrency);
        
        log.info("Updating exchange rate: fromCurrency={}, rate={}", normalizedCurrency, rate);
        
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_RATE", "Exchange rate must be greater than zero");
        }
        
        Long userId = SecurityUtils.getCurrentUserId();
        
        ExchangeRate exchangeRate = exchangeRateRepository.findByFromCurrency(normalizedCurrency)
                .orElseGet(() -> {
                    log.info("Creating new exchange rate: fromCurrency={}, toCurrency={}", normalizedCurrency, EUR_CURRENCY);
                    ExchangeRate newRate = new ExchangeRate();
                    newRate.setFromCurrency(normalizedCurrency);
                    newRate.setToCurrency(EUR_CURRENCY);
                    return newRate;
                });
        
        boolean isNew = exchangeRate.getId() == null;
        exchangeRate.setRate(rate);
        exchangeRate.setUpdatedByUserId(userId);
        
        ExchangeRate saved = exchangeRateRepository.save(exchangeRate);
        log.info("Exchange rate {}: id={}, fromCurrency={}, rate={}, updatedByUserId={}", 
                isNew ? "created" : "updated", saved.getId(), saved.getFromCurrency(), saved.getRate(), userId);
        
        return saved;
    }
    
    private String normalizeCurrency(@NonNull String currency) {
        if (currency.trim().isEmpty()) {
            throw new PurchaseException("INVALID_CURRENCY", "Currency cannot be empty");
        }
        return currency.trim().toUpperCase();
    }

}
