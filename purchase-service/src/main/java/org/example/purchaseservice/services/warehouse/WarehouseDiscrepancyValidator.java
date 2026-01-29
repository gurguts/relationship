package org.example.purchaseservice.services.warehouse;

import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Slf4j
@Service
public class WarehouseDiscrepancyValidator {
    
    private static final int MAX_PAGE_SIZE = 1000;
    private static final Set<String> VALID_DISCREPANCY_TYPES = Set.of("LOSS", "GAIN");
    
    public void validateDiscrepancyParams(
            Long driverId,
            Long productId,
            Long warehouseId,
            LocalDate receiptDate,
            BigDecimal purchasedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal unitPriceEur) {
        
        if (driverId == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Driver ID cannot be null");
        }
        if (productId == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Product ID cannot be null");
        }
        if (warehouseId == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Warehouse ID cannot be null");
        }
        if (receiptDate == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Receipt date cannot be null");
        }
        if (purchasedQuantity == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Purchased quantity cannot be null");
        }
        if (receivedQuantity == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Received quantity cannot be null");
        }
        if (unitPriceEur == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Unit price cannot be null");
        }
        
        if (purchasedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Purchased quantity cannot be negative");
        }
        if (receivedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Received quantity cannot be negative");
        }
        if (unitPriceEur.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Unit price must be positive");
        }
    }
    
    public void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new PurchaseException("INVALID_DATE_RANGE",
                    String.format("Date from (%s) cannot be after date to (%s)", dateFrom, dateTo));
        }
    }
    
    public void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new PurchaseException("INVALID_PAGEABLE", "Pageable cannot be null");
        }
        if (pageable.getPageNumber() < 0) {
            throw new PurchaseException("INVALID_PAGE", 
                    String.format("Page number cannot be negative, got: %d", pageable.getPageNumber()));
        }
        if (pageable.getPageSize() <= 0) {
            throw new PurchaseException("INVALID_PAGE_SIZE", "Page size must be positive");
        }
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new PurchaseException("INVALID_PAGE_SIZE",
                    String.format("Page size cannot exceed %d, got: %d", MAX_PAGE_SIZE, pageable.getPageSize()));
        }
    }
    
    public void validateDiscrepancyType(String type) {
        if (type != null && !type.trim().isEmpty() && !VALID_DISCREPANCY_TYPES.contains(type.toUpperCase())) {
            throw new PurchaseException("INVALID_DISCREPANCY_TYPE",
                    String.format("Invalid discrepancy type: %s. Valid types: %s",
                            type, String.join(", ", VALID_DISCREPANCY_TYPES)));
        }
    }
    
    public void validateDriverBalance(org.example.purchaseservice.models.balance.DriverProductBalance driverBalance) {
        if (driverBalance.getDriverId() == null) {
            throw new PurchaseException("INVALID_DRIVER_BALANCE", "Driver ID cannot be null");
        }
        if (driverBalance.getProductId() == null) {
            throw new PurchaseException("INVALID_DRIVER_BALANCE", "Product ID cannot be null");
        }
        if (driverBalance.getQuantity() == null) {
            throw new PurchaseException("INVALID_DRIVER_BALANCE", "Quantity cannot be null");
        }
        if (driverBalance.getAveragePriceEur() == null) {
            throw new PurchaseException("INVALID_DRIVER_BALANCE", "Average price cannot be null");
        }
    }
}
