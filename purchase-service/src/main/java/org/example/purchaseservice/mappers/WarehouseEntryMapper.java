package org.example.purchaseservice.mappers;

import org.example.purchaseservice.models.dto.warehouse.WarehouseEntryCreateDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseEntryDTO;
import org.example.purchaseservice.models.warehouse.WarehouseEntry;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WarehouseEntryMapper {
    
    @Autowired
    private WithdrawalReasonRepository withdrawalReasonRepository;
    public WarehouseEntry warehouseEntryCreateDTOToWarehouseEntry(WarehouseEntryCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        WarehouseEntry warehouseEntry = new WarehouseEntry();
        warehouseEntry.setUserId(dto.getUserId());
        warehouseEntry.setProductId(dto.getProductId());
        warehouseEntry.setWarehouseId(dto.getWarehouseId());
        warehouseEntry.setQuantity(dto.getQuantity());
        warehouseEntry.setEntryDate(dto.getEntryDate());

        if (dto.getTypeId() != null) {
            WithdrawalReason type = withdrawalReasonRepository.findById(dto.getTypeId()).orElse(null);
            warehouseEntry.setType(type);
        } else {
            WithdrawalReason defaultType = withdrawalReasonRepository.findByPurpose(WithdrawalReason.Purpose.ADDING)
                .stream()
                .findFirst()
                .orElse(null);
            warehouseEntry.setType(defaultType);
        }
        
        return warehouseEntry;
    }

    public WarehouseEntryDTO warehouseEntryToWarehouseEntryDTO(WarehouseEntry warehouseEntry) {
        if (warehouseEntry == null) {
            return null;
        }

        WarehouseEntryDTO warehouseEntryDTO = new WarehouseEntryDTO();
        warehouseEntryDTO.setId(warehouseEntry.getId());
        warehouseEntryDTO.setUserId(warehouseEntry.getUserId());
        warehouseEntryDTO.setProductId(warehouseEntry.getProductId());
        warehouseEntryDTO.setWarehouseId(warehouseEntry.getWarehouseId());
        warehouseEntryDTO.setQuantity(warehouseEntry.getQuantity());
        warehouseEntryDTO.setEntryDate(warehouseEntry.getEntryDate());
        warehouseEntryDTO.setType(warehouseEntry.getType());

        return warehouseEntryDTO;
    }
}
