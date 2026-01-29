package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseWithdrawCalculator {
    
    public static final int QUANTITY_SCALE = 2;
    public static final int PRICE_SCALE = PriceResolver.PRICE_SCALE;
    public static final RoundingMode QUANTITY_ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final RoundingMode PRICE_ROUNDING_MODE = PriceResolver.PRICE_ROUNDING_MODE;
    
    private final PriceResolver priceResolver;
    
    public BigDecimal validateAndScaleQuantity(@NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity must be positive");
        }
        return quantity.setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE);
    }
    
    public BigDecimal calculateTotalCost(@NonNull BigDecimal unitPrice, @NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
        }
        return unitPrice.multiply(quantity).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
    }
    
    public BigDecimal resolveUnitPrice(@NonNull WarehouseWithdrawal withdrawal) {
        return priceResolver.resolveUnitPrice(
                withdrawal.getUnitPriceEur(),
                withdrawal.getQuantity(),
                withdrawal.getTotalCostEur(),
                withdrawal.getId(),
                "Withdrawal"
        );
    }
}
