package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.warehouse.ProductTransfer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTransferCalculator {
    
    public static final int PRICE_SCALE = PriceResolver.PRICE_SCALE;
    public static final RoundingMode PRICE_ROUNDING_MODE = PriceResolver.PRICE_ROUNDING_MODE;
    
    private final PriceResolver priceResolver;
    
    public BigDecimal calculateTotalCost(@NonNull BigDecimal quantity, @NonNull BigDecimal unitPrice) {
        return quantity.multiply(unitPrice).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
    }
    
    public BigDecimal resolveUnitPrice(@NonNull ProductTransfer transfer) {
        return priceResolver.resolveUnitPrice(
                transfer.getUnitPriceEur(),
                transfer.getQuantity(),
                transfer.getTotalCostEur(),
                transfer.getId(),
                "Transfer"
        );
    }
}
