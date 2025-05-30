package org.example.purchaseservice.mappers;

import org.example.purchaseservice.models.dto.warehouse.WarehouseEntryCreateDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseEntryDTO;
import org.example.purchaseservice.models.warehouse.WarehouseEntry;
import org.springframework.stereotype.Component;

@Component
public class WarehouseEntryMapper {
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

        return warehouseEntryDTO;
    }
}
