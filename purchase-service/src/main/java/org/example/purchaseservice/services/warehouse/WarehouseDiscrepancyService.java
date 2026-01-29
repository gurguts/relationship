package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.models.dto.warehouse.DiscrepancyStatisticsDTO;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.example.purchaseservice.repositories.WarehouseDiscrepancyRepository;
import org.example.purchaseservice.services.impl.IWarehouseDiscrepancyService;
import org.example.purchaseservice.services.warehouse.WarehouseDiscrepancyExcelDataFetcher.ExcelData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseDiscrepancyService implements IWarehouseDiscrepancyService {
    
    private final WarehouseDiscrepancyRepository discrepancyRepository;
    private final WarehouseDiscrepancyValidator validator;
    private final WarehouseDiscrepancySpecificationBuilder specificationBuilder;
    private final WarehouseDiscrepancyFactory factory;
    private final WarehouseDiscrepancyStatisticsCalculator statisticsCalculator;
    private final WarehouseDiscrepancyExcelDataFetcher excelDataFetcher;
    private final WarehouseDiscrepancyExcelGenerator excelGenerator;

    @Override
    @Transactional
    public void createDiscrepancy(
            Long warehouseReceiptId,
            Long driverId,
            Long productId,
            Long warehouseId,
            LocalDate receiptDate,
            BigDecimal purchasedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal unitPriceEur,
            Long createdByUserId,
            String comment) {
        
        validator.validateDiscrepancyParams(driverId, productId, warehouseId, receiptDate, 
                purchasedQuantity, receivedQuantity, unitPriceEur);
        
        WarehouseDiscrepancy discrepancy = factory.createDiscrepancy(
                warehouseReceiptId, driverId, productId, warehouseId, receiptDate,
                purchasedQuantity, receivedQuantity, unitPriceEur, createdByUserId, comment);
        
        if (discrepancy == null) {
            return;
        }
        
        log.info("Creating warehouse discrepancy: driverId={}, productId={}, warehouseId={}, type={}, quantity={}", 
                driverId, productId, warehouseId, discrepancy.getType(), discrepancy.getDiscrepancyQuantity());
        
        WarehouseDiscrepancy saved = discrepancyRepository.save(discrepancy);
        log.info("Warehouse discrepancy created: id={}", saved.getId());
    }
    
    @Override
    @Transactional
    public void createFromDriverBalance(
            @NonNull Long warehouseReceiptId,
            @NonNull DriverProductBalance driverBalance,
            @NonNull Long warehouseId,
            @NonNull LocalDate receiptDate,
            @NonNull BigDecimal receivedQuantity,
            Long createdByUserId,
            String comment) {
        
        validator.validateDriverBalance(driverBalance);

        createDiscrepancy(
                warehouseReceiptId,
                driverBalance.getDriverId(),
                driverBalance.getProductId(),
                warehouseId,
                receiptDate,
                driverBalance.getQuantity(),
                receivedQuantity,
                driverBalance.getAveragePriceEur(),
                createdByUserId,
                comment
        );
    }

    @Transactional(readOnly = true)
    public Page<WarehouseDiscrepancy> getDiscrepancies(
            Long driverId,
            Long productId,
            Long warehouseId,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable) {
        
        validator.validateDateRange(dateFrom, dateTo);
        validator.validatePageable(pageable);
        validator.validateDiscrepancyType(type);
        
        Specification<WarehouseDiscrepancy> spec = specificationBuilder.buildSpecification(
                driverId, productId, warehouseId, type, dateFrom, dateTo);
        return discrepancyRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscrepancyStatisticsDTO getStatistics(String type, LocalDate dateFrom, LocalDate dateTo) {
        validator.validateDateRange(dateFrom, dateTo);
        validator.validateDiscrepancyType(type);
        
        Specification<WarehouseDiscrepancy> spec = specificationBuilder.buildSpecification(
                null, null, null, type, dateFrom, dateTo);
        List<WarehouseDiscrepancy> filteredDiscrepancies = discrepancyRepository.findAll(spec);

        return statisticsCalculator.calculateStatistics(filteredDiscrepancies);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseDiscrepancy getDiscrepancyById(@NonNull Long id) {
        return discrepancyRepository.findById(id)
                .orElseThrow(() -> new PurchaseException("DISCREPANCY_NOT_FOUND",
                        String.format("Discrepancy not found: id=%d", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel(
            Long driverId,
            Long productId,
            Long warehouseId,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo) throws IOException {
        
        validator.validateDateRange(dateFrom, dateTo);
        validator.validateDiscrepancyType(type);
        
        Specification<WarehouseDiscrepancy> spec = specificationBuilder.buildSpecification(
                driverId, productId, warehouseId, type, dateFrom, dateTo);
        List<WarehouseDiscrepancy> discrepancies = discrepancyRepository.findAll(spec);
        
        ExcelData excelData = excelDataFetcher.loadExcelData(discrepancies);
        return excelGenerator.generateExcel(discrepancies, excelData);
    }
}
