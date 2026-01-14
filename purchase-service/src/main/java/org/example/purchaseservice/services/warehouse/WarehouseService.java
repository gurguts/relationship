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
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(String.format("Warehouse with ID %d not found", id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "warehouses", key = "'allWarehouses'")
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"warehouses"}, allEntries = true)
    public Warehouse createWarehouse(@NonNull Warehouse warehouse) {
        log.info("Creating new warehouse: name={}", warehouse.getName());
        validateWarehouse(warehouse);
        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Warehouse created: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"warehouses"}, allEntries = true)
    public Warehouse updateWarehouse(@NonNull Long id, @NonNull Warehouse warehouse) {
        log.info("Updating warehouse: id={}", id);
        validateWarehouse(warehouse);
        Warehouse existingWarehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(String.format("Warehouse with ID %d not found", id)));

        if (warehouse.getName() != null) {
            existingWarehouse.setName(warehouse.getName());
        }
        if (warehouse.getDescription() != null) {
            existingWarehouse.setDescription(warehouse.getDescription());
        }

        Warehouse saved = warehouseRepository.save(existingWarehouse);
        log.info("Warehouse updated: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"warehouses"}, allEntries = true)
    public void deleteWarehouse(@NonNull Long id) {
        log.info("Deleting warehouse: id={}", id);
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(String.format("Warehouse with ID %d not found", id)));
        warehouseRepository.delete(warehouse);
        log.info("Warehouse deleted: id={}", id);
    }

    private void validateWarehouse(@NonNull Warehouse warehouse) {
        if (warehouse.getName() == null || warehouse.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse name cannot be null or empty");
        }
    }
}
