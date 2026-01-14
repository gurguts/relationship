package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.models.dto.warehouse.DiscrepancyStatisticsDTO;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface IWarehouseDiscrepancyService {
    void createDiscrepancy(
            Long warehouseReceiptId,
            Long driverId,
            Long productId,
            Long warehouseId,
            LocalDate receiptDate,
            BigDecimal purchasedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal unitPriceEur,
            Long createdByUserId,
            String comment);
    
    void createFromDriverBalance(
            @NonNull Long warehouseReceiptId,
            @NonNull DriverProductBalance driverBalance,
            @NonNull Long warehouseId,
            @NonNull LocalDate receiptDate,
            @NonNull BigDecimal receivedQuantity,
            Long createdByUserId,
            String comment);
    
    Page<WarehouseDiscrepancy> getDiscrepancies(
            Long driverId,
            Long productId,
            Long warehouseId,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable);
    
    DiscrepancyStatisticsDTO getStatistics(String type, LocalDate dateFrom, LocalDate dateTo);
    
    WarehouseDiscrepancy getDiscrepancyById(@NonNull Long id);
    
    byte[] exportToExcel(
            Long driverId,
            Long productId,
            Long warehouseId,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo) throws IOException;
}
