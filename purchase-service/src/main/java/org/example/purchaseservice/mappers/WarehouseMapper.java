package org.example.purchaseservice.mappers;

import lombok.NonNull;
import org.example.purchaseservice.models.dto.warehouse.WarehouseCreateDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseUpdateDTO;
import org.example.purchaseservice.models.warehouse.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class WarehouseMapper {

    public WarehouseDTO warehouseToWarehouseDTO(@NonNull Warehouse warehouse) {
        WarehouseDTO dto = new WarehouseDTO();
        dto.setId(warehouse.getId());
        dto.setName(warehouse.getName());
        dto.setDescription(warehouse.getDescription());
        return dto;
    }

    public Warehouse warehouseCreateDTOToWarehouse(@NonNull WarehouseCreateDTO dto) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(dto.getName());
        warehouse.setDescription(dto.getDescription());
        return warehouse;
    }

    public Warehouse warehouseUpdateDTOToWarehouse(@NonNull WarehouseUpdateDTO dto) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(dto.getName());
        warehouse.setDescription(dto.getDescription());
        return warehouse;
    }
}
