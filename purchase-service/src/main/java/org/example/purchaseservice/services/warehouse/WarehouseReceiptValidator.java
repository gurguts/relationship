package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.WarehouseException;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WarehouseReceiptValidator {
    
    private static final int MAX_PAGE_SIZE = 1000;
    
    public void validateWarehouseReceipt(@NonNull WarehouseReceipt warehouseReceipt) {
        if (warehouseReceipt.getUserId() == null) {
            throw new WarehouseException("INVALID_RECEIPT", "User ID cannot be null");
        }
        if (warehouseReceipt.getProductId() == null) {
            throw new WarehouseException("INVALID_RECEIPT", "Product ID cannot be null");
        }
        if (warehouseReceipt.getWarehouseId() == null) {
            throw new WarehouseException("INVALID_RECEIPT", "Warehouse ID cannot be null");
        }
        if (warehouseReceipt.getQuantity() == null) {
            throw new WarehouseException("INVALID_RECEIPT", "Quantity cannot be null");
        }
        if (warehouseReceipt.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new WarehouseException("INVALID_RECEIPT", "Quantity must be positive");
        }
    }
    
    public void validateDriverBalance(DriverProductBalance driverBalance, Long userId, Long productId) {
        if (driverBalance == null) {
            throw new WarehouseException("INSUFFICIENT_DRIVER_BALANCE", 
                    String.format("Driver %d doesn't have product %d in balance", userId, productId));
        }
        if (driverBalance.getQuantity() == null) {
            throw new WarehouseException("INVALID_DRIVER_BALANCE", "Driver balance quantity cannot be null");
        }
        if (driverBalance.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new WarehouseException("INSUFFICIENT_DRIVER_BALANCE", 
                    String.format("Driver %d doesn't have product %d in balance", userId, productId));
        }
        if (driverBalance.getTotalCostEur() == null) {
            throw new WarehouseException("INVALID_DRIVER_BALANCE", "Driver balance total cost cannot be null");
        }
        if (driverBalance.getAveragePriceEur() == null) {
            throw new WarehouseException("INVALID_DRIVER_BALANCE", "Driver balance average price cannot be null");
        }
    }
    
    public void validatePage(int page) {
        if (page < 0) {
            throw new WarehouseException("INVALID_PAGE", 
                    String.format("Page number cannot be negative, got: %d", page));
        }
    }
    
    public void validatePageSize(int size) {
        if (size <= 0) {
            throw new WarehouseException("INVALID_PAGE_SIZE", "Page size must be positive");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new WarehouseException("INVALID_PAGE_SIZE",
                    String.format("Page size cannot exceed %d, got: %d", MAX_PAGE_SIZE, size));
        }
    }
    
    public void validateSortParams(String sort, String direction) {
        if (sort == null || sort.trim().isEmpty()) {
            throw new WarehouseException("INVALID_SORT", "Sort parameter cannot be null or empty");
        }
        if (direction == null || direction.trim().isEmpty()) {
            throw new WarehouseException("INVALID_SORT_DIRECTION", "Sort direction cannot be null or empty");
        }
        try {
            org.springframework.data.domain.Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            throw new WarehouseException("INVALID_SORT_DIRECTION",
                    String.format("Invalid sort direction: %s. Valid values: ASC, DESC", direction));
        }
    }
    
    public void validateFilters(Map<String, List<String>> filters) {
        if (filters == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new WarehouseException("INVALID_FILTER", "Filter keys cannot be null or empty");
            }
            if (entry.getValue() != null) {
                for (String value : entry.getValue()) {
                    if (value == null || value.trim().isEmpty()) {
                        throw new WarehouseException("INVALID_FILTER", "Filter values cannot be null or empty");
                    }
                }
            }
        }
    }
}
