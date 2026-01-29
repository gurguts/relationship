package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class WarehouseWithdrawValidator {
    
    private static final BigDecimal MINIMUM_REMAINING = BigDecimal.ONE;
    
    public void validateWarehouseWithdrawal(@NonNull WarehouseWithdrawal warehouseWithdrawal) {
        if (warehouseWithdrawal.getWarehouseId() == null) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Warehouse ID cannot be null");
        }
        if (warehouseWithdrawal.getProductId() == null) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Product ID cannot be null");
        }
        if (warehouseWithdrawal.getQuantity() == null) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Quantity cannot be null");
        }
        if (warehouseWithdrawal.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Quantity must be positive");
        }
    }

    public void validateWarehouseWithdrawalForUpdate(@NonNull WarehouseWithdrawal warehouseWithdrawal) {
        if (warehouseWithdrawal.getQuantity() != null && warehouseWithdrawal.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Quantity cannot be negative");
        }
    }
    
    public void validateQuantity(@NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be negative");
        }
    }
    
    public WarehouseProductBalance validateAndGetBalance(@NonNull WarehouseProductBalance balance) {

        if (balance.getQuantity() == null) {
            throw new PurchaseException("INVALID_BALANCE", "Balance quantity cannot be null");
        }
        
        if (balance.getAveragePriceEur() == null) {
            throw new PurchaseException("INVALID_BALANCE", "Balance average price cannot be null");
        }
        
        return balance;
    }
    
    public void validateAvailableQuantity(
            @NonNull BigDecimal availableQuantity,
            @NonNull BigDecimal requestedQuantity,
            Long vehicleId) {
        
        if (availableQuantity.compareTo(requestedQuantity) < 0) {
            throw new PurchaseException("INSUFFICIENT_PRODUCT", 
                    String.format("Insufficient product on warehouse. Available: %s, requested: %s",
                            availableQuantity, requestedQuantity));
        }
        
        if (vehicleId == null) {
            validateMinimumRemaining(availableQuantity, requestedQuantity);
        }
    }
    
    public void validateMinimumRemaining(
            @NonNull BigDecimal availableQuantity,
            @NonNull BigDecimal requestedQuantity) {
        
        BigDecimal remainingAfterWithdrawal = availableQuantity.subtract(requestedQuantity);
        if (remainingAfterWithdrawal.compareTo(MINIMUM_REMAINING) < 0) {
            BigDecimal maxAllowedQuantity = availableQuantity.subtract(MINIMUM_REMAINING);
            throw new PurchaseException("INSUFFICIENT_PRODUCT", 
                    String.format("Cannot withdraw all product. At least %s unit must remain on warehouse. Available: %s, maximum allowed withdrawal: %s",
                            MINIMUM_REMAINING, availableQuantity, maxAllowedQuantity));
        }
    }
    
    public void validateProductChange(@NonNull WarehouseWithdrawal request, @NonNull WarehouseWithdrawal withdrawal) {
        if (request.getProductId() != null && !request.getProductId().equals(withdrawal.getProductId())) {
            throw new PurchaseException("PRODUCT_CHANGE_NOT_ALLOWED", 
                    "Cannot change product for existing withdrawal");
        }
    }
}
