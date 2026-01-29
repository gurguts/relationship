package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseDiscrepancyFactory {
    
    private final WarehouseDiscrepancyCalculator calculator;
    
    public WarehouseDiscrepancy createDiscrepancy(
            Long warehouseReceiptId,
            @NonNull Long driverId,
            @NonNull Long productId,
            @NonNull Long warehouseId,
            @NonNull LocalDate receiptDate,
            @NonNull BigDecimal purchasedQuantity,
            @NonNull BigDecimal receivedQuantity,
            @NonNull BigDecimal unitPriceEur,
            Long createdByUserId,
            String comment) {
        
        BigDecimal discrepancyQuantity = calculator.calculateDiscrepancyQuantity(receivedQuantity, purchasedQuantity);
        
        if (discrepancyQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        BigDecimal discrepancyValue = calculator.calculateDiscrepancyValue(discrepancyQuantity, unitPriceEur);
        WarehouseDiscrepancy.DiscrepancyType type = calculator.determineDiscrepancyType(discrepancyQuantity);
        
        return WarehouseDiscrepancy.builder()
                .warehouseReceiptId(warehouseReceiptId)
                .driverId(driverId)
                .productId(productId)
                .warehouseId(warehouseId)
                .receiptDate(receiptDate)
                .purchasedQuantity(purchasedQuantity)
                .receivedQuantity(receivedQuantity)
                .discrepancyQuantity(discrepancyQuantity)
                .unitPriceEur(unitPriceEur)
                .discrepancyValueEur(discrepancyValue)
                .type(type)
                .comment(comment)
                .createdByUserId(createdByUserId)
                .build();
    }
}
