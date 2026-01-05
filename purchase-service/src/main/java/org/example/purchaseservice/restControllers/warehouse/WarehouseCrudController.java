package org.example.purchaseservice.restControllers.warehouse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.mappers.WarehouseMapper;
import org.example.purchaseservice.models.dto.warehouse.WarehouseCreateDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseUpdateDTO;
import org.example.purchaseservice.services.impl.IWarehouseService;
import org.example.purchaseservice.models.warehouse.Warehouse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WarehouseCrudController {
    private final WarehouseMapper warehouseMapper;
    private final IWarehouseService warehouseService;

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDTO> getWarehouse(@PathVariable @Positive Long id) {
        WarehouseDTO warehouseDTO = warehouseMapper.warehouseToWarehouseDTO(warehouseService.getWarehouse(id));
        return ResponseEntity.ok(warehouseDTO);
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping
    public ResponseEntity<List<WarehouseDTO>> getWarehouses() {
        List<Warehouse> warehouses = warehouseService.getAllWarehouses();
        List<WarehouseDTO> dtos = warehouses.stream()
                .map(warehouseMapper::warehouseToWarehouseDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('system:admin')")
    @PostMapping
    public ResponseEntity<WarehouseDTO> createWarehouse(@RequestBody @Valid @NonNull WarehouseCreateDTO warehouseCreateDTO) {
        Warehouse warehouse = warehouseMapper.warehouseCreateDTOToWarehouse(warehouseCreateDTO);
        WarehouseDTO createdWarehouse = warehouseMapper.warehouseToWarehouseDTO(warehouseService.createWarehouse(warehouse));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdWarehouse.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdWarehouse);
    }

    @PreAuthorize("hasAuthority('system:admin')")
    @PatchMapping("/{id}")
    public ResponseEntity<WarehouseDTO> updateWarehouse(
            @PathVariable @Positive Long id,
            @RequestBody @Valid @NonNull WarehouseUpdateDTO warehouseUpdateDTO) {
        Warehouse warehouse = warehouseMapper.warehouseUpdateDTOToWarehouse(warehouseUpdateDTO);
        Warehouse updatedWarehouse = warehouseService.updateWarehouse(id, warehouse);
        return ResponseEntity.ok(warehouseMapper.warehouseToWarehouseDTO(updatedWarehouse));
    }

    @PreAuthorize("hasAuthority('system:admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable @Positive Long id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}
