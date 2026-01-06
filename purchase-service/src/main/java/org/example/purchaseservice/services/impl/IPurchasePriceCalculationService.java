package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.Purchase;

import java.math.BigDecimal;

public interface IPurchasePriceCalculationService {
    
    void calculateAndSetUnitPrice(@NonNull Purchase purchase);
    
    void calculateAndSetPricesInEur(@NonNull Purchase purchase, BigDecimal exchangeRateToEur);
}

