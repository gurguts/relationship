package org.example.purchaseservice.services.balance;

import org.example.purchaseservice.models.balance.AdjustmentType;

import java.math.BigDecimal;

public class WarehouseBalanceUpdateRecords {
    
    public record AdjustmentUpdateResult(
            BigDecimal updatedQuantity,
            BigDecimal updatedTotalCost,
            BigDecimal updatedAverage,
            AdjustmentType adjustmentType
    ) {}
    
    public record QuantityUpdateResult(
            BigDecimal updatedQuantity,
            BigDecimal updatedTotalCost
    ) {}
    
    public record TotalCostUpdateResult(
            BigDecimal updatedQuantity,
            BigDecimal updatedTotalCost
    ) {}
    
    private WarehouseBalanceUpdateRecords() {
        throw new UnsupportedOperationException("Utility class");
    }
}

