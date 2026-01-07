package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.WarehouseNotFoundException;
import org.example.purchaseservice.models.warehouse.Warehouse;
import org.example.purchaseservice.repositories.WarehouseRepository;
import org.example.purchaseservice.services.impl.IWarehouseService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService implements IWarehouseService {
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "warehouses", key = "#id")
    public Warehouse getWarehouse(@NonNull Long id) {
        try {
            return warehouseRepository.findById(id)
                    .orElseThrow(() -> new WarehouseNotFoundException(String.format("Warehouse with ID %d not found", id)));
        } catch (WarehouseNotFoundException e) {
            log.error("Warehouse not found: id={}", id, e);
            throw e;
        } catch (Exception e) {
            log.error("Error getting warehouse: id={}", id, e);
            throw new WarehouseNotFoundException(String.format("Warehouse with ID %d not found", id));
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "warehouses", key = "'allWarehouses'")
    public List<Warehouse> getAllWarehouses() {
        try {
            return warehouseRepository.findAll();
        } catch (Exception e) {
            log.error("Error getting all warehouses", e);
            throw new RuntimeException("Failed to get all warehouses", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"warehouses"}, allEntries = true)
    public Warehouse createWarehouse(@NonNull Warehouse warehouse) {
        validateWarehouse(warehouse);
        try {
            return warehouseRepository.save(warehouse);
        } catch (Exception e) {
            log.error("Error creating warehouse: name={}", warehouse.getName(), e);
            throw new RuntimeException("Failed to create warehouse", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"warehouses"}, allEntries = true)
    public Warehouse updateWarehouse(@NonNull Long id, @NonNull Warehouse warehouse) {
        validateWarehouse(warehouse);
        try {
            Warehouse existingWarehouse = warehouseRepository.findById(id)
                    .orElseThrow(() -> new WarehouseNotFoundException(String.format("Warehouse with ID %d not found", id)));
            
            if (warehouse.getName() != null) {
                existingWarehouse.setName(warehouse.getName());
            }
            if (warehouse.getDescription() != null) {
                existingWarehouse.setDescription(warehouse.getDescription());
            }
            
            return warehouseRepository.save(existingWarehouse);
        } catch (WarehouseNotFoundException e) {
            log.error("Warehouse not found for update: id={}", id, e);
            throw e;
        } catch (Exception e) {
            log.error("Error updating warehouse: id={}", id, e);
            throw new RuntimeException("Failed to update warehouse", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"warehouses"}, allEntries = true)
    public void deleteWarehouse(@NonNull Long id) {
        try {
            Warehouse warehouse = warehouseRepository.findById(id)
                    .orElseThrow(() -> new WarehouseNotFoundException(String.format("Warehouse with ID %d not found", id)));
            warehouseRepository.delete(warehouse);
        } catch (WarehouseNotFoundException e) {
            log.error("Warehouse not found for deletion: id={}", id, e);
            throw e;
        } catch (Exception e) {
            log.error("Error deleting warehouse: id={}", id, e);
            throw new RuntimeException("Failed to delete warehouse", e);
        }
    }

    private void validateWarehouse(@NonNull Warehouse warehouse) {
        if (warehouse.getName() == null || warehouse.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse name cannot be null or empty");
        }
    }
}
