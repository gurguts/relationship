package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseProductBalanceRepository extends JpaRepository<WarehouseProductBalance, Long> {

    Optional<WarehouseProductBalance> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    List<WarehouseProductBalance> findByWarehouseId(Long warehouseId);

    List<WarehouseProductBalance> findByProductId(Long productId);

    @Query("SELECT wpb FROM WarehouseProductBalance wpb WHERE wpb.quantity > 0")
    List<WarehouseProductBalance> findAllWithPositiveQuantity();

    @Query("SELECT wpb FROM WarehouseProductBalance wpb WHERE wpb.warehouseId = :warehouseId AND wpb.quantity > 0")
    List<WarehouseProductBalance> findByWarehouseIdWithPositiveQuantity(@Param("warehouseId") Long warehouseId);
}

