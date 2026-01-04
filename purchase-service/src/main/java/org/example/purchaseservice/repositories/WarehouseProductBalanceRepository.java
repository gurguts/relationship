package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WarehouseProductBalanceRepository extends JpaRepository<WarehouseProductBalance, Long> {
    Optional<WarehouseProductBalance> findByWarehouseIdAndProductId(@NonNull Long warehouseId, @NonNull Long productId);

    @NonNull
    List<WarehouseProductBalance> findByWarehouseId(@NonNull Long warehouseId);

    @NonNull
    List<WarehouseProductBalance> findByProductId(@NonNull Long productId);

    @Query("SELECT wpb FROM WarehouseProductBalance wpb WHERE wpb.quantity > 0")
    @NonNull
    List<WarehouseProductBalance> findAllWithPositiveQuantity();
}
