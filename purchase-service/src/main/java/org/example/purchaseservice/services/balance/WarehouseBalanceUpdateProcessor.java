package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.AdjustmentType;
import org.example.purchaseservice.services.balance.WarehouseBalanceUpdateRecords.AdjustmentUpdateResult;
import org.example.purchaseservice.services.balance.WarehouseBalanceUpdateRecords.QuantityUpdateResult;
import org.example.purchaseservice.services.balance.WarehouseBalanceUpdateRecords.TotalCostUpdateResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class WarehouseBalanceUpdateProcessor {
    
    private final WarehouseProductBalanceCalculator calculator;
    private final WarehouseProductBalanceValidator validator;
    private final WarehouseBalanceAdjustmentService adjustmentService;
    
    private static final int PRICE_SCALE = 6;
    private static final int QUANTITY_SCALE = 2;
    private static final RoundingMode QUANTITY_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    public AdjustmentUpdateResult processAdjustmentUpdates(BigDecimal newQuantity, BigDecimal newTotalCost,
                                                          BigDecimal previousQuantity, BigDecimal previousTotalCost,
                                                          BigDecimal unitPrice) {
        validator.validateChangesProvided(newQuantity, newTotalCost);
        
        QuantityUpdateResult quantityResult = processQuantityUpdate(
                newQuantity, newTotalCost, previousQuantity, previousTotalCost, unitPrice);
        
        TotalCostUpdateResult totalCostResult = processTotalCostUpdate(
                newTotalCost, quantityResult.updatedQuantity(), quantityResult.updatedTotalCost());
        
        AdjustmentType adjustmentType = adjustmentService.determineAdjustmentType(newQuantity, newTotalCost);
        
        BigDecimal[] averageAndTotalCost = calculator.calculateUpdatedAverageAndTotalCost(
                totalCostResult.updatedQuantity(), totalCostResult.updatedTotalCost());
        
        return new AdjustmentUpdateResult(
                totalCostResult.updatedQuantity(),
                averageAndTotalCost[1],
                averageAndTotalCost[0],
                adjustmentType);
    }
    
    private QuantityUpdateResult processQuantityUpdate(BigDecimal newQuantity, BigDecimal newTotalCost,
                                                      BigDecimal previousQuantity, BigDecimal previousTotalCost,
                                                      BigDecimal unitPrice) {
        BigDecimal updatedQuantity = previousQuantity;
        BigDecimal updatedTotalCost = previousTotalCost;
        
        if (newQuantity != null) {
            updatedQuantity = newQuantity.setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE);
            validator.validateQuantityNonNegative(updatedQuantity);
            
            if (newTotalCost == null) {
                if (updatedQuantity.compareTo(BigDecimal.ZERO) == 0) {
                    updatedTotalCost = BigDecimal.ZERO.setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);
                } else {
                    updatedTotalCost = calculator.calculateTotalCostFromQuantity(updatedQuantity, unitPrice);
                }
            }
        }
        
        return new QuantityUpdateResult(updatedQuantity, updatedTotalCost);
    }
    
    private TotalCostUpdateResult processTotalCostUpdate(BigDecimal newTotalCost, BigDecimal updatedQuantity,
                                                        BigDecimal calculatedTotalCost) {
        BigDecimal updatedTotalCost = calculatedTotalCost;
        
        if (newTotalCost != null) {
            BigDecimal total = newTotalCost.setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);
            validator.validateTotalCostNonNegative(total, "Updated");
            updatedTotalCost = total;
        }
        
        return new TotalCostUpdateResult(updatedQuantity, updatedTotalCost);
    }
}
