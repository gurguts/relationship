package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.services.impl.IVehicleService;
import org.example.purchaseservice.services.impl.IWarehouseProductBalanceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseWithdrawBalanceHandler {
    
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    private final IVehicleService vehicleService;
    private final WarehouseWithdrawCalculator calculator;
    private final WarehouseWithdrawValidator validator;
    
    public record PriceCalculationResult(BigDecimal unitPrice, BigDecimal totalCost) {}
    
    public PriceCalculationResult calculatePriceAndRemoveProduct(
            @NonNull WarehouseWithdrawal warehouseWithdrawal,
            @NonNull WarehouseProductBalance balance,
            @NonNull BigDecimal quantity) {
        
        BigDecimal unitPrice;
        BigDecimal totalCost;
        
        if (warehouseWithdrawal.getVehicleId() != null) {
            unitPrice = balance.getAveragePriceEur();
            totalCost = calculator.calculateTotalCost(unitPrice, quantity);
            
            warehouseProductBalanceService.removeProductWithCost(
                    warehouseWithdrawal.getWarehouseId(),
                    warehouseWithdrawal.getProductId(),
                    quantity,
                    totalCost
            );
        } else {
            BigDecimal newAveragePrice = warehouseProductBalanceService.removeProduct(
                    warehouseWithdrawal.getWarehouseId(),
                    warehouseWithdrawal.getProductId(),
                    quantity
            );
            
            if (newAveragePrice == null) {
                throw new PurchaseException("INVALID_BALANCE", "Average price cannot be null after removal");
            }
            
            unitPrice = newAveragePrice.setScale(WarehouseWithdrawCalculator.PRICE_SCALE, 
                    WarehouseWithdrawCalculator.PRICE_ROUNDING_MODE);
            totalCost = calculator.calculateTotalCost(unitPrice, quantity);
        }
        
        return new PriceCalculationResult(unitPrice, totalCost);
    }
    
    public void handleQuantityIncrease(
            @NonNull WarehouseWithdrawal withdrawal,
            @NonNull BigDecimal delta,
            @NonNull BigDecimal unitPrice,
            @NonNull WarehouseProductBalance balance) {
        
        BigDecimal availableQuantity = balance.getQuantity();
        
        if (availableQuantity.compareTo(delta) < 0) {
            throw new PurchaseException("INSUFFICIENT_PRODUCT", 
                    String.format("Insufficient product on warehouse. Available: %s, requested additionally: %s",
                            availableQuantity, delta));
        }
        
        BigDecimal additionalQuantity = delta.setScale(WarehouseWithdrawCalculator.QUANTITY_SCALE, 
                WarehouseWithdrawCalculator.QUANTITY_ROUNDING_MODE);
        BigDecimal additionalCost = calculator.calculateTotalCost(unitPrice, additionalQuantity);
        
        if (withdrawal.getVehicleId() != null) {
            warehouseProductBalanceService.removeProductWithCost(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    additionalQuantity,
                    additionalCost
            );
            vehicleService.addWithdrawalCost(withdrawal.getVehicleId(), additionalCost);
        } else {
            validator.validateMinimumRemaining(availableQuantity, delta);
            
            warehouseProductBalanceService.removeProduct(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    additionalQuantity
            );
        }
    }
    
    public void handleQuantityDecrease(
            @NonNull WarehouseWithdrawal withdrawal,
            @NonNull BigDecimal quantityToReturn,
            @NonNull BigDecimal unitPrice) {
        
        BigDecimal scaledQuantity = quantityToReturn.setScale(WarehouseWithdrawCalculator.QUANTITY_SCALE, 
                WarehouseWithdrawCalculator.QUANTITY_ROUNDING_MODE);
        BigDecimal costToReturn = calculator.calculateTotalCost(unitPrice, scaledQuantity);
        
        addProductToWarehouse(withdrawal, scaledQuantity, costToReturn);
    }
    
    public void restoreWithdrawalToWarehouse(@NonNull WarehouseWithdrawal withdrawal) {
        BigDecimal quantity = withdrawal.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal scaledQuantity = quantity.setScale(WarehouseWithdrawCalculator.QUANTITY_SCALE, 
                WarehouseWithdrawCalculator.QUANTITY_ROUNDING_MODE);
        BigDecimal unitPrice = calculator.resolveUnitPrice(withdrawal);
        BigDecimal totalCost = calculator.calculateTotalCost(unitPrice, scaledQuantity);

        addProductToWarehouse(withdrawal, scaledQuantity, totalCost);
    }
    
    private void addProductToWarehouse(
            @NonNull WarehouseWithdrawal withdrawal,
            @NonNull BigDecimal scaledQuantity,
            @NonNull BigDecimal totalCost) {
        
        if (withdrawal.getVehicleId() != null) {
            warehouseProductBalanceService.addProduct(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    scaledQuantity,
                    totalCost
            );
            vehicleService.subtractWithdrawalCost(withdrawal.getVehicleId(), totalCost);
        } else {
            warehouseProductBalanceService.addProductQuantityOnly(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    scaledQuantity
            );
        }
    }
}
