package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.WarehouseBalanceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseBalanceAdjustmentRepository extends JpaRepository<WarehouseBalanceAdjustment, Long> {
    List<WarehouseBalanceAdjustment> findByWarehouseIdAndProductIdOrderByCreatedAtDesc(Long warehouseId, Long productId);
}
