package org.example.purchaseservice.restControllers.warehouse;

import jakarta.validation.Valid;
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

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDTO> getWarehouse(@PathVariable Long id) {
        WarehouseDTO warehouseDTO = warehouseMapper.warehouseToWarehouseDTO(warehouseService.getWarehouse(id));
        return ResponseEntity.ok(warehouseDTO);
    }

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
    public ResponseEntity<WarehouseDTO> createWarehouse(@RequestBody @Valid WarehouseCreateDTO warehouseCreateDTO) {
        Warehouse warehouse = warehouseMapper.warehouseCreateDTOToWarehouse(warehouseCreateDTO);
        WarehouseDTO createdWarehouse = warehouseMapper.warehouseToWarehouseDTO(warehouseService.createWarehouse(warehouse));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdWarehouse.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdWarehouse);
    }

    @PreAuthorize("hasAuthority('system:admin')")
    @PutMapping("/{id}")
    public ResponseEntity<WarehouseDTO> updateWarehouse(@PathVariable Long id,
                                                      @RequestBody @Valid WarehouseUpdateDTO businessUpdateDTO) {
        Warehouse warehouse = warehouseMapper.warehouseUpdateDTOToWarehouse(businessUpdateDTO);
        Warehouse updatedBusiness = warehouseService.updateWarehouse(id, warehouse);
        return ResponseEntity.ok(warehouseMapper.warehouseToWarehouseDTO(updatedBusiness));
    }

    @PreAuthorize("hasAuthority('system:admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}
