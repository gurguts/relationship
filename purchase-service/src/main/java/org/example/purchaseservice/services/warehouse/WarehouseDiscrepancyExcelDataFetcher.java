package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.warehouse.Warehouse;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.repositories.WarehouseRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseDiscrepancyExcelDataFetcher {
    
    private static final String UNKNOWN_PRODUCT = "Невідомо";
    private static final String UNKNOWN_WAREHOUSE = "Невідомо";
    
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    
    public record ExcelData(
            Map<Long, String> productNames,
            Map<Long, String> warehouseNames
    ) {}
    
    public ExcelData loadExcelData(@NonNull List<WarehouseDiscrepancy> discrepancies) {
        Map<Long, String> productNames = loadProductNames(discrepancies);
        Map<Long, String> warehouseNames = loadWarehouseNames(discrepancies);
        return new ExcelData(productNames, warehouseNames);
    }
    
    private Map<Long, String> loadProductNames(@NonNull List<WarehouseDiscrepancy> discrepancies) {
        Set<Long> productIds = discrepancies.stream()
                .map(WarehouseDiscrepancy::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return StreamSupport.stream(productRepository.findAllById(productIds).spliterator(), false)
                .collect(Collectors.toMap(Product::getId, Product::getName, (v1, _) -> v1));
    }
    
    private Map<Long, String> loadWarehouseNames(@NonNull List<WarehouseDiscrepancy> discrepancies) {
        Set<Long> warehouseIds = discrepancies.stream()
                .map(WarehouseDiscrepancy::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (warehouseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return warehouseRepository.findAllById(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName, (v1, _) -> v1));
    }
    
    public String getProductName(@NonNull Long productId, @NonNull Map<Long, String> productNames) {
        return productNames.getOrDefault(productId, UNKNOWN_PRODUCT);
    }
    
    public String getWarehouseName(@NonNull Long warehouseId, @NonNull Map<Long, String> warehouseNames) {
        return warehouseNames.getOrDefault(warehouseId, UNKNOWN_WAREHOUSE);
    }
}
