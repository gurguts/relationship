package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.warehouse.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
}