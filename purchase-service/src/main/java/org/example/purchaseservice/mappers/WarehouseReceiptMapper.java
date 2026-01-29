package org.example.purchaseservice.mappers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.dto.warehouse.WarehouseReceiptCreateDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseReceiptDTO;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarehouseReceiptMapper {
    
    private final WithdrawalReasonRepository withdrawalReasonRepository;
    
    public WarehouseReceipt warehouseReceiptCreateDTOToWarehouseReceipt(@NonNull WarehouseReceiptCreateDTO dto) {
        WarehouseReceipt warehouseReceipt = new WarehouseReceipt();
        warehouseReceipt.setUserId(dto.getUserId());
        warehouseReceipt.setProductId(dto.getProductId());
        warehouseReceipt.setWarehouseId(dto.getWarehouseId());
        warehouseReceipt.setQuantity(dto.getQuantity());

        if (dto.getTypeId() != null) {
            WithdrawalReason type = withdrawalReasonRepository.findById(dto.getTypeId()).orElse(null);
            warehouseReceipt.setType(type);
        } else {
            WithdrawalReason defaultType = withdrawalReasonRepository.findByPurpose(WithdrawalReason.Purpose.ADDING)
                .stream()
                .findFirst()
                .orElse(null);
            warehouseReceipt.setType(defaultType);
        }
        
        return warehouseReceipt;
    }

    public WarehouseReceiptDTO warehouseReceiptToWarehouseReceiptDTO(@NonNull WarehouseReceipt warehouseReceipt) {
        WarehouseReceiptDTO warehouseReceiptDTO = new WarehouseReceiptDTO();
        warehouseReceiptDTO.setId(warehouseReceipt.getId());
        warehouseReceiptDTO.setUserId(warehouseReceipt.getUserId());
        warehouseReceiptDTO.setProductId(warehouseReceipt.getProductId());
        warehouseReceiptDTO.setWarehouseId(warehouseReceipt.getWarehouseId());
        warehouseReceiptDTO.setQuantity(warehouseReceipt.getQuantity());
        warehouseReceiptDTO.setDriverBalanceQuantity(warehouseReceipt.getDriverBalanceQuantity());
        warehouseReceiptDTO.setEntryDate(warehouseReceipt.getEntryDate());
        warehouseReceiptDTO.setType(warehouseReceipt.getType());
        warehouseReceiptDTO.setPurchasedQuantity(warehouseReceipt.getDriverBalanceQuantity());
        warehouseReceiptDTO.setUnitPriceEur(warehouseReceipt.getUnitPriceEur());
        warehouseReceiptDTO.setTotalCostEur(warehouseReceipt.getTotalCostEur());
        return warehouseReceiptDTO;
    }
}
