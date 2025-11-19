package org.example.purchaseservice.mappers;

import org.example.purchaseservice.models.dto.warehouse.WarehouseReceiptCreateDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseReceiptDTO;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WarehouseReceiptMapper {
    
    @Autowired
    private WithdrawalReasonRepository withdrawalReasonRepository;
    
    public WarehouseReceipt warehouseReceiptCreateDTOToWarehouseReceipt(WarehouseReceiptCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        WarehouseReceipt warehouseReceipt = new WarehouseReceipt();
        warehouseReceipt.setUserId(dto.getUserId());
        warehouseReceipt.setProductId(dto.getProductId());
        warehouseReceipt.setWarehouseId(dto.getWarehouseId());
        warehouseReceipt.setQuantity(dto.getQuantity());
        warehouseReceipt.setEntryDate(dto.getEntryDate());

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

    public WarehouseReceiptDTO warehouseReceiptToWarehouseReceiptDTO(WarehouseReceipt warehouseReceipt) {
        if (warehouseReceipt == null) {
            return null;
        }

        WarehouseReceiptDTO warehouseReceiptDTO = new WarehouseReceiptDTO();
        warehouseReceiptDTO.setId(warehouseReceipt.getId());
        warehouseReceiptDTO.setUserId(warehouseReceipt.getUserId());
        warehouseReceiptDTO.setProductId(warehouseReceipt.getProductId());
        warehouseReceiptDTO.setWarehouseId(warehouseReceipt.getWarehouseId());
        warehouseReceiptDTO.setQuantity(warehouseReceipt.getQuantity());
        warehouseReceiptDTO.setEntryDate(warehouseReceipt.getEntryDate());
        warehouseReceiptDTO.setType(warehouseReceipt.getType());
        warehouseReceiptDTO.setUnitPriceUah(warehouseReceipt.getUnitPriceUah());
        warehouseReceiptDTO.setTotalCostUah(warehouseReceipt.getTotalCostUah());

        return warehouseReceiptDTO;
    }
}

