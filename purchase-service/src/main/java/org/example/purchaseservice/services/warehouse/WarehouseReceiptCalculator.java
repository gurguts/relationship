package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.WarehouseException;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Slf4j
@Service
public class WarehouseReceiptCalculator {
    
    public static final int PRICE_SCALE = 6;
    public static final RoundingMode PRICE_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    public BigDecimal calculateWarehouseUnitPrice(@NonNull BigDecimal totalDriverCost, @NonNull BigDecimal receivedQuantity) {
        if (receivedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WarehouseException("INVALID_QUANTITY", "Received quantity must be positive");
        }
        return totalDriverCost.divide(receivedQuantity, PRICE_SCALE, PRICE_ROUNDING_MODE);
    }
    
    public void prepareWarehouseReceipt(
            @NonNull WarehouseReceipt warehouseReceipt,
            @NonNull BigDecimal purchasedQuantity,
            @NonNull BigDecimal warehouseUnitPrice,
            @NonNull BigDecimal totalDriverCost,
            @NonNull Long executorUserId) {
        
        warehouseReceipt.setDriverBalanceQuantity(purchasedQuantity);
        warehouseReceipt.setUnitPriceEur(warehouseUnitPrice);
        warehouseReceipt.setTotalCostEur(totalDriverCost);
        warehouseReceipt.setExecutorUserId(executorUserId);
        
        if (warehouseReceipt.getEntryDate() == null) {
            warehouseReceipt.setEntryDate(LocalDate.now());
        }
    }
    
    public LocalDate getReceiptDate(@NonNull WarehouseReceipt savedReceipt) {
        if (savedReceipt.getEntryDate() != null) {
            return savedReceipt.getEntryDate();
        }
        if (savedReceipt.getCreatedAt() != null) {
            return savedReceipt.getCreatedAt().toLocalDate();
        }
        return LocalDate.now();
    }
}
