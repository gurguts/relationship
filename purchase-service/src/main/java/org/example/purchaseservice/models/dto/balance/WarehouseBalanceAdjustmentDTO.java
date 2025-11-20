package org.example.purchaseservice.models.dto.balance;

import lombok.Builder;
import lombok.Value;
import org.example.purchaseservice.models.balance.WarehouseBalanceAdjustment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class WarehouseBalanceAdjustmentDTO {
    Long id;
    Long warehouseId;
    Long productId;
    BigDecimal previousQuantity;
    BigDecimal newQuantity;
    BigDecimal previousTotalCostEur;
    BigDecimal newTotalCostEur;
    BigDecimal previousAveragePriceEur;
    BigDecimal newAveragePriceEur;
    WarehouseBalanceAdjustment.AdjustmentType adjustmentType;
    String description;
    Long userId;
    LocalDateTime createdAt;
}
