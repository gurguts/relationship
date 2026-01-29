package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class WarehouseDiscrepancyCalculator {
    
    public static final int VALUE_SCALE = 2;
    public static final RoundingMode VALUE_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    public BigDecimal calculateDiscrepancyValue(@NonNull BigDecimal discrepancyQuantity, @NonNull BigDecimal unitPriceEur) {
        return discrepancyQuantity.multiply(unitPriceEur)
                .setScale(VALUE_SCALE, VALUE_ROUNDING_MODE);
    }
    
    public BigDecimal calculateDiscrepancyQuantity(@NonNull BigDecimal receivedQuantity, @NonNull BigDecimal purchasedQuantity) {
        return receivedQuantity.subtract(purchasedQuantity);
    }
    
    public WarehouseDiscrepancy.DiscrepancyType determineDiscrepancyType(@NonNull BigDecimal discrepancyQuantity) {
        return discrepancyQuantity.compareTo(BigDecimal.ZERO) > 0 
                ? WarehouseDiscrepancy.DiscrepancyType.GAIN 
                : WarehouseDiscrepancy.DiscrepancyType.LOSS;
    }
}
