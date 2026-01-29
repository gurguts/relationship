package org.example.purchaseservice.services.warehouse;

import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
public class PriceResolver {
    
    public static final int PRICE_SCALE = 6;
    public static final RoundingMode PRICE_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    public BigDecimal resolveUnitPrice(
            BigDecimal unitPrice,
            BigDecimal quantity,
            BigDecimal totalCost,
            Long entityId,
            String entityType) {
        
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            return unitPrice.setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
        }

        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0
                && totalCost != null && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            return totalCost.divide(quantity, PRICE_SCALE, PRICE_ROUNDING_MODE);
        }

        throw new PurchaseException(entityType + "_PRICE_MISSING",
                String.format("%s %d is missing price information", entityType, entityId));
    }
}
