package org.example.purchaseservice.services.warehouse;

import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.WarehouseNotFoundException;
import org.example.purchaseservice.models.warehouse.Warehouse;
import org.example.purchaseservice.repositories.WarehouseRepository;
import org.example.purchaseservice.services.impl.IWarehouseService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class WarehouseService implements IWarehouseService {
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "warehouses", key = "#id")
    public Warehouse getWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(String.format("Warehouse with ID %d not found", id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "warehouses", key = "'allWarehousess'")
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"warehouses"}, allEntries = true)
    public Warehouse createWarehouse(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"warehouses"}, allEntries = true)
    public Warehouse updateWarehouse(Long id, Warehouse warehouse) {
        Warehouse existingWarehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(String.format("Warehouse with ID %d not found", id)));
        existingWarehouse.setName(warehouse.getName());
        existingWarehouse.setDescription(warehouse.getDescription());
        return warehouseRepository.save(existingWarehouse);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"warehouses"}, allEntries = true)
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(String.format("Product with ID %d not found", id)));
        warehouseRepository.delete(warehouse);
    }
}
