package org.example.purchaseservice.mappers;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.WarehouseBalanceAdjustment;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.models.dto.balance.WarehouseBalanceAdjustmentDTO;
import org.example.purchaseservice.models.dto.balance.WarehouseProductBalanceDTO;
import org.springframework.stereotype.Component;

@Component
public class WarehouseBalanceMapper {

    public WarehouseProductBalanceDTO warehouseProductBalanceToWarehouseProductBalanceDTO(@NonNull WarehouseProductBalance balance) {
        return WarehouseProductBalanceDTO.builder()
                .id(balance.getId())
                .warehouseId(balance.getWarehouseId())
                .productId(balance.getProductId())
                .quantity(balance.getQuantity())
                .averagePriceEur(balance.getAveragePriceEur())
                .totalCostEur(balance.getTotalCostEur())
                .updatedAt(balance.getUpdatedAt())
                .build();
    }

    public WarehouseBalanceAdjustmentDTO warehouseBalanceAdjustmentToWarehouseBalanceAdjustmentDTO(@NonNull WarehouseBalanceAdjustment adjustment) {
        return WarehouseBalanceAdjustmentDTO.builder()
                .id(adjustment.getId())
                .warehouseId(adjustment.getWarehouseId())
                .productId(adjustment.getProductId())
                .previousQuantity(adjustment.getPreviousQuantity())
                .newQuantity(adjustment.getNewQuantity())
                .previousTotalCostEur(adjustment.getPreviousTotalCostEur())
                .newTotalCostEur(adjustment.getNewTotalCostEur())
                .previousAveragePriceEur(adjustment.getPreviousAveragePriceEur())
                .newAveragePriceEur(adjustment.getNewAveragePriceEur())
                .adjustmentType(adjustment.getAdjustmentType())
                .description(adjustment.getDescription())
                .userId(adjustment.getUserId())
                .createdAt(adjustment.getCreatedAt())
                .build();
    }
}

