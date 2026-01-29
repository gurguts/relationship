package org.example.purchaseservice.services.balance;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DriverProductBalanceCalculator {

    private static final int PRICE_SCALE = 6;

    public BigDecimal calculateAveragePrice(@NonNull BigDecimal totalCost, @NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            return totalCost.divide(quantity, PRICE_SCALE, RoundingMode.CEILING);
        }
        return BigDecimal.ZERO;
    }

    public void updateBalanceValues(@NonNull DriverProductBalance balance, 
                                   @NonNull BigDecimal newQuantity, 
                                   @NonNull BigDecimal newTotalCost) {
        balance.setQuantity(newQuantity);
        balance.setTotalCostEur(newTotalCost);
        balance.setAveragePriceEur(calculateAveragePrice(newTotalCost, newQuantity));
    }

    public BigDecimal ensureNonNegative(BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value != null ? value : BigDecimal.ZERO;
    }
}
