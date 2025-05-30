package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.warehouse.Warehouse;

import java.util.List;

public interface IWarehouseService {
    Warehouse getWarehouse(Long id);

    List<Warehouse> getAllWarehouses();

    Warehouse createWarehouse(Warehouse warehouse);

    Warehouse updateWarehouse(Long id, Warehouse warehouse);

    void deleteWarehouse(Long id);
}
