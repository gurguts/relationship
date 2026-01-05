package org.example.purchaseservice.mappers;

import lombok.NonNull;
import org.example.purchaseservice.models.dto.warehouse.WarehouseDiscrepancyDTO;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.springframework.stereotype.Component;

@Component
public class WarehouseDiscrepancyMapper {

    public WarehouseDiscrepancyDTO warehouseDiscrepancyToWarehouseDiscrepancyDTO(@NonNull WarehouseDiscrepancy discrepancy) {
        return WarehouseDiscrepancyDTO.builder()
                .id(discrepancy.getId())
                .warehouseReceiptId(discrepancy.getWarehouseReceiptId())
                .driverId(discrepancy.getDriverId())
                .productId(discrepancy.getProductId())
                .warehouseId(discrepancy.getWarehouseId())
                .receiptDate(discrepancy.getReceiptDate())
                .purchasedQuantity(discrepancy.getPurchasedQuantity())
                .receivedQuantity(discrepancy.getReceivedQuantity())
                .discrepancyQuantity(discrepancy.getDiscrepancyQuantity())
                .unitPriceEur(discrepancy.getUnitPriceEur())
                .discrepancyValueEur(discrepancy.getDiscrepancyValueEur())
                .type(discrepancy.getType().name())
                .comment(discrepancy.getComment())
                .createdByUserId(discrepancy.getCreatedByUserId())
                .createdAt(discrepancy.getCreatedAt())
                .build();
    }
}

