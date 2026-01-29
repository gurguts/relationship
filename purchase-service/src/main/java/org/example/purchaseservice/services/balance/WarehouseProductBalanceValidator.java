package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WarehouseProductBalanceValidator {
    
    public void validateQuantityPositive(BigDecimal quantity, String operation) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", 
                    String.format("%s quantity must be positive", operation));
        }
    }
    
    public void validateQuantityNonNegative(BigDecimal quantity) {
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be negative");
        }
    }
    
    public void validateTotalCostNonNegative(BigDecimal totalCost, String operation) {
        if (totalCost != null && totalCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TOTAL_COST", 
                    String.format("%s total cost must be non-negative", operation));
        }
    }
    
    public void validateAveragePriceNonNegative(BigDecimal averagePrice) {
        if (averagePrice != null && averagePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_AVERAGE_PRICE", "Average price cannot be negative");
        }
    }
    
    public void validateSufficientQuantity(WarehouseProductBalance balance, BigDecimal requiredQuantity, 
                                          WarehouseProductBalanceHelper helper) {
        BigDecimal availableQuantity = helper.getSafeQuantity(balance);
        if (requiredQuantity.compareTo(availableQuantity) > 0) {
            throw new PurchaseException("INSUFFICIENT_QUANTITY", 
                    String.format("Cannot remove more than available quantity. Available: %s, trying to remove: %s", 
                            availableQuantity, requiredQuantity));
        }
    }
    
    public void validateCostAdjustmentResult(BigDecimal newTotalCost) {
        if (newTotalCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_COST_ADJUSTMENT", "Resulting total cost cannot be negative");
        }
    }
    
    public void validateBalanceDoesNotExist(boolean exists, Long warehouseId, Long productId) {
        if (exists) {
            throw new PurchaseException("BALANCE_ALREADY_EXISTS", 
                    String.format("Balance already exists for warehouse %d, product %d. Use update instead.", 
                            warehouseId, productId));
        }
    }
    
    public void validateChangesProvided(BigDecimal newQuantity, BigDecimal newTotalCost) {
        if (newQuantity == null && newTotalCost == null) {
            throw new PurchaseException("NO_CHANGES", "No changes were provided");
        }
    }
}
