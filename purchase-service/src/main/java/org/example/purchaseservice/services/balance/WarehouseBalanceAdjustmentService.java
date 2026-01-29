package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.AdjustmentType;
import org.example.purchaseservice.models.balance.WarehouseBalanceAdjustment;
import org.example.purchaseservice.repositories.WarehouseBalanceAdjustmentRepository;
import org.example.purchaseservice.services.balance.WarehouseBalanceUpdateRecords.AdjustmentUpdateResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WarehouseBalanceAdjustmentService {
    
    private final WarehouseBalanceAdjustmentRepository warehouseBalanceAdjustmentRepository;
    
    public void createBalanceAdjustment(Long warehouseId, Long productId, Long userId, String description,
                                       BigDecimal previousQuantity, BigDecimal previousTotalCost, BigDecimal previousAverage,
                                       AdjustmentUpdateResult updateResult) {
        WarehouseBalanceAdjustment adjustment = new WarehouseBalanceAdjustment();
        adjustment.setWarehouseId(warehouseId);
        adjustment.setProductId(productId);
        adjustment.setPreviousQuantity(previousQuantity);
        adjustment.setNewQuantity(updateResult.updatedQuantity());
        adjustment.setPreviousTotalCostEur(previousTotalCost);
        adjustment.setNewTotalCostEur(updateResult.updatedTotalCost());
        adjustment.setPreviousAveragePriceEur(previousAverage);
        adjustment.setNewAveragePriceEur(updateResult.updatedAverage());
        adjustment.setAdjustmentType(updateResult.adjustmentType());
        adjustment.setDescription(description);
        adjustment.setUserId(userId);
        warehouseBalanceAdjustmentRepository.save(adjustment);
    }
    
    public AdjustmentType determineAdjustmentType(BigDecimal newQuantity, BigDecimal newTotalCost) {
        if (newQuantity != null && newTotalCost != null) {
            return AdjustmentType.BOTH;
        }
        return newQuantity != null ? AdjustmentType.QUANTITY : AdjustmentType.TOTAL_COST;
    }
}
