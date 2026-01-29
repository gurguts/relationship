package org.example.purchaseservice.mappers;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalDTO;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WarehouseWithdrawMapper {
    
    public WithdrawalDTO convertToDTO(@NonNull WarehouseWithdrawal withdrawal) {
        if (withdrawal.getQuantity() == null) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Withdrawal quantity cannot be null");
        }
        
        return WithdrawalDTO.builder()
                .id(withdrawal.getId())
                .productId(withdrawal.getProductId())
                .warehouseId(withdrawal.getWarehouseId())
                .userId(withdrawal.getUserId())
                .withdrawalReason(withdrawal.getWithdrawalReason())
                .quantity(withdrawal.getQuantity().doubleValue())
                .unitPriceEur(withdrawal.getUnitPriceEur())
                .totalCostEur(withdrawal.getTotalCostEur())
                .description(withdrawal.getDescription())
                .withdrawalDate(withdrawal.getWithdrawalDate())
                .createdAt(withdrawal.getCreatedAt())
                .build();
    }
}
