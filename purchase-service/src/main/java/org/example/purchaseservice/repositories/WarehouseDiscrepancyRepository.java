package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface WarehouseDiscrepancyRepository extends JpaRepository<WarehouseDiscrepancy, Long>, JpaSpecificationExecutor<WarehouseDiscrepancy> {
    
    /**
     * Find all discrepancies for specific warehouse receipt
     */
    List<WarehouseDiscrepancy> findByWarehouseReceiptId(Long warehouseReceiptId);
    
    /**
     * Find all discrepancies by driver
     */
    List<WarehouseDiscrepancy> findByDriverId(Long driverId);
    
    /**
     * Find all discrepancies by product
     */
    List<WarehouseDiscrepancy> findByProductId(Long productId);
    
    /**
     * Find all discrepancies by warehouse
     */
    List<WarehouseDiscrepancy> findByWarehouseId(Long warehouseId);
    
    /**
     * Find all discrepancies by type (LOSS or GAIN)
     */
    List<WarehouseDiscrepancy> findByType(WarehouseDiscrepancy.DiscrepancyType type);
    
    /**
     * Find discrepancies within date range
     */
    @Query("SELECT wd FROM WarehouseDiscrepancy wd WHERE wd.receiptDate BETWEEN :startDate AND :endDate ORDER BY wd.receiptDate DESC")
    List<WarehouseDiscrepancy> findByReceiptDateBetween(@Param("startDate") LocalDate startDate, 
                                                         @Param("endDate") LocalDate endDate);
    
    /**
     * Get total losses value
     */
    @Query("SELECT COALESCE(SUM(wd.discrepancyValueEur), 0) FROM WarehouseDiscrepancy wd WHERE wd.type = 'LOSS'")
    BigDecimal getTotalLossesValue();
    
    /**
     * Get total gains value
     */
    @Query("SELECT COALESCE(SUM(wd.discrepancyValueEur), 0) FROM WarehouseDiscrepancy wd WHERE wd.type = 'GAIN'")
    BigDecimal getTotalGainsValue();
}

