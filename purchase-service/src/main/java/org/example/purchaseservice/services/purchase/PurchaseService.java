package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.services.impl.IPurchasePriceCalculationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService implements IPurchasePriceCalculationService {
    
    private static final int PRICE_SCALE = 6;
    private static final String EUR_CURRENCY = "EUR";
    private static final RoundingMode UNIT_PRICE_ROUNDING_MODE = RoundingMode.CEILING;
    private static final RoundingMode EXCHANGE_RATE_ROUNDING_MODE = RoundingMode.HALF_UP;

    @Override
    public void calculateAndSetUnitPrice(@NonNull Purchase purchase) {
        BigDecimal quantity = purchase.getQuantity();
        BigDecimal totalPrice = purchase.getTotalPrice();

        validateNonNegative(quantity, "quantity");
        validateNonNegative(totalPrice, "totalPrice");

        if (isValidForUnitPriceCalculation(quantity, totalPrice)) {
            BigDecimal unitPrice = totalPrice.divide(quantity, PRICE_SCALE, UNIT_PRICE_ROUNDING_MODE);
            purchase.setUnitPrice(unitPrice);
        } else {
            purchase.setUnitPrice(BigDecimal.ZERO);
        }
    }

    private boolean isValidForUnitPriceCalculation(BigDecimal quantity, BigDecimal totalPrice) {
        return quantity != null && totalPrice != null && quantity.compareTo(BigDecimal.ZERO) > 0;
    }

    private void validateNonNegative(BigDecimal value, String fieldName) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    String.format("Value of %s cannot be negative. Provided value: %s", fieldName, value));
        }
    }

    @Override
    public void calculateAndSetPricesInEur(@NonNull Purchase purchase, BigDecimal exchangeRateToEur) {
        BigDecimal totalPrice = purchase.getTotalPrice();
        BigDecimal quantity = purchase.getQuantity();

        if (totalPrice == null || quantity == null) {
            purchase.setTotalPriceEur(null);
            purchase.setUnitPriceEur(null);
            return;
        }

        validateNonNegative(totalPrice, "totalPrice");
        validateNonNegative(quantity, "quantity");
        if (exchangeRateToEur != null) {
            validateNonNegative(exchangeRateToEur, "exchangeRateToEur");
        }

        if (isEurCurrency(purchase.getCurrency())) {
            setPricesDirectly(purchase, totalPrice, quantity);
        } else {
            calculatePricesWithExchangeRate(purchase, totalPrice, quantity, exchangeRateToEur);
        }
    }

    private boolean isEurCurrency(String currency) {
        if (currency == null) {
            return true;
        }
        return EUR_CURRENCY.equalsIgnoreCase(currency);
    }

    private void setPricesDirectly(@NonNull Purchase purchase, @NonNull BigDecimal totalPrice, @NonNull BigDecimal quantity) {
        purchase.setTotalPriceEur(totalPrice);
        purchase.setUnitPriceEur(calculateUnitPriceEur(totalPrice, quantity));
    }

    private void calculatePricesWithExchangeRate(@NonNull Purchase purchase, @NonNull BigDecimal totalPrice,
                                                  @NonNull BigDecimal quantity, BigDecimal exchangeRateToEur) {
        if (isValidExchangeRate(exchangeRateToEur)) {
            BigDecimal totalPriceEur = totalPrice.divide(exchangeRateToEur, PRICE_SCALE, EXCHANGE_RATE_ROUNDING_MODE);
            purchase.setTotalPriceEur(totalPriceEur);
            purchase.setUnitPriceEur(calculateUnitPriceEur(totalPriceEur, quantity));
        } else {
            setPricesDirectly(purchase, totalPrice, quantity);
        }
    }

    private boolean isValidExchangeRate(BigDecimal exchangeRateToEur) {
        return exchangeRateToEur != null && exchangeRateToEur.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calculateUnitPriceEur(@NonNull BigDecimal totalPriceEur, @NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) != 0) {
            return totalPriceEur.divide(quantity, PRICE_SCALE, UNIT_PRICE_ROUNDING_MODE);
        }
        return BigDecimal.ZERO;
    }
}

