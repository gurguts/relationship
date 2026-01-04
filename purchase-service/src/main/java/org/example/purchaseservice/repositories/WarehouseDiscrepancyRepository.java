package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface WarehouseDiscrepancyRepository extends JpaRepository<WarehouseDiscrepancy, Long>, JpaSpecificationExecutor<WarehouseDiscrepancy> {
    @NonNull
    List<WarehouseDiscrepancy> findByWarehouseReceiptId(@NonNull Long warehouseReceiptId);

    @NonNull
    List<WarehouseDiscrepancy> findByDriverId(@NonNull Long driverId);

    @NonNull
    List<WarehouseDiscrepancy> findByProductId(@NonNull Long productId);

    @NonNull
    List<WarehouseDiscrepancy> findByWarehouseId(@NonNull Long warehouseId);

    @NonNull
    List<WarehouseDiscrepancy> findByType(@NonNull WarehouseDiscrepancy.DiscrepancyType type);

    @Query("SELECT wd FROM WarehouseDiscrepancy wd WHERE wd.receiptDate BETWEEN :startDate AND :endDate ORDER BY wd.receiptDate DESC")
    @NonNull
    List<WarehouseDiscrepancy> findByReceiptDateBetween(@Param("startDate") @NonNull LocalDate startDate,
                                                         @Param("endDate") @NonNull LocalDate endDate);

    @Query("SELECT COALESCE(SUM(wd.discrepancyValueEur), 0) FROM WarehouseDiscrepancy wd WHERE wd.type = 'LOSS'")
    @NonNull
    BigDecimal getTotalLossesValue();

    @Query("SELECT COALESCE(SUM(wd.discrepancyValueEur), 0) FROM WarehouseDiscrepancy wd WHERE wd.type = 'GAIN'")
    @NonNull
    BigDecimal getTotalGainsValue();
}
