package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.WarehouseBalanceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseBalanceAdjustmentRepository extends JpaRepository<WarehouseBalanceAdjustment, Long> {
    @NonNull
    List<WarehouseBalanceAdjustment> findByWarehouseIdAndProductIdOrderByCreatedAtDesc(@NonNull Long warehouseId, @NonNull Long productId);
}
