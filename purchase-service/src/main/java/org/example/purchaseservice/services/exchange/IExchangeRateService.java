package org.example.purchaseservice.services.exchange;

import lombok.NonNull;
import org.example.purchaseservice.models.ExchangeRate;

import java.math.BigDecimal;
import java.util.List;

public interface IExchangeRateService {
    
    BigDecimal getExchangeRateToEur(@NonNull String fromCurrency);
    
    List<ExchangeRate> getAllExchangeRates();
    
    ExchangeRate updateExchangeRate(@NonNull String fromCurrency, @NonNull BigDecimal rate);
}
