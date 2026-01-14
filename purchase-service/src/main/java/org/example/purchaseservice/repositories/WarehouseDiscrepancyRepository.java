package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WarehouseDiscrepancyRepository extends JpaRepository<WarehouseDiscrepancy, Long>, JpaSpecificationExecutor<WarehouseDiscrepancy> {

}
