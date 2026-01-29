package org.example.purchaseservice.services.balance;

import lombok.NonNull;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DriverProductBalanceValidator {

    public void validateQuantity(@NonNull BigDecimal quantity, String operation) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", 
                    String.format("%s quantity must be positive", operation));
        }
    }

    public void validateQuantityNonNegative(BigDecimal quantity, String fieldName) {
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_QUANTITY", 
                    String.format("%s must be non-negative", fieldName));
        }
    }

    public void validateTotalPrice(@NonNull BigDecimal totalPrice, String operation) {
        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TOTAL_PRICE", 
                    String.format("%s total price must be non-negative", operation));
        }
    }

    public void validateTotalPriceNonNegative(BigDecimal totalPrice, String fieldName) {
        if (totalPrice != null && totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TOTAL_PRICE", 
                    String.format("%s must be non-negative", fieldName));
        }
    }

    public void validateSufficientQuantity(@NonNull BigDecimal availableQuantity, @NonNull BigDecimal requestedQuantity) {
        if (requestedQuantity.compareTo(availableQuantity) > 0) {
            throw new PurchaseException("INSUFFICIENT_QUANTITY",
                    String.format("Cannot remove more than available quantity. Available: %s, trying to remove: %s",
                            availableQuantity, requestedQuantity));
        }
    }
}
