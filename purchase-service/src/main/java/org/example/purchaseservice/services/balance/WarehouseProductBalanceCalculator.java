package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class WarehouseProductBalanceCalculator {
    
    private static final int PRICE_SCALE = 6;
    private static final RoundingMode PRICE_ROUNDING_MODE = RoundingMode.CEILING;
    
    public BigDecimal calculateAveragePrice(BigDecimal totalCost, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (totalCost == null) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(quantity, PRICE_SCALE, PRICE_ROUNDING_MODE);
    }
    
    public BigDecimal resolveUnitPrice(BigDecimal quantity, BigDecimal totalCost) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        }
        if (totalCost == null) {
            return BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        }
        return totalCost.divide(quantity, PRICE_SCALE, RoundingMode.HALF_UP);
    }
    
    public BigDecimal calculateTotalCostFromQuantity(BigDecimal quantity, BigDecimal unitPrice) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        }
        return unitPrice.multiply(quantity).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }
    
    public BigDecimal[] calculateUpdatedAverageAndTotalCost(BigDecimal updatedQuantity, BigDecimal updatedTotalCost) {
        BigDecimal updatedAverage;
        BigDecimal finalTotalCost = updatedTotalCost;
        
        if (updatedQuantity.compareTo(BigDecimal.ZERO) > 0) {
            updatedAverage = calculateAveragePrice(updatedTotalCost, updatedQuantity);
        } else {
            updatedAverage = BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            finalTotalCost = BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        }
        
        return new BigDecimal[]{updatedAverage, finalTotalCost};
    }
}
