package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferResponseDTO;
import org.example.purchaseservice.models.dto.warehouse.TransferExcelData;
import org.example.purchaseservice.models.warehouse.Warehouse;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.repositories.WarehouseRepository;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTransferExcelDataFetcher {
    
    private static final String UNKNOWN_WAREHOUSE = "Невідомо";
    private static final String UNKNOWN_PRODUCT = "Невідомо";
    private static final String UNKNOWN_REASON = "Не вказано";
    
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final WithdrawalReasonRepository withdrawalReasonRepository;
    
    public List<TransferExcelData> prepareExcelData(@NonNull List<ProductTransferResponseDTO> transfers) {
        if (transfers.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<Long> warehouseIds = extractWarehouseIds(transfers);
        Set<Long> productIds = extractProductIds(transfers);
        Set<Long> reasonIds = extractReasonIds(transfers);
        
        Map<Long, String> warehouseNames = loadWarehouseNames(warehouseIds);
        Map<Long, String> productNames = loadProductNames(productIds);
        Map<Long, String> reasonNames = loadReasonNames(reasonIds);
        
        return transfers.stream()
                .map(transfer -> buildTransferExcelData(transfer, warehouseNames, productNames, reasonNames))
                .toList();
    }
    
    private Set<Long> extractWarehouseIds(@NonNull List<ProductTransferResponseDTO> transfers) {
        return transfers.stream()
                .map(ProductTransferResponseDTO::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
    
    private Set<Long> extractProductIds(@NonNull List<ProductTransferResponseDTO> transfers) {
        return transfers.stream()
                .flatMap(t -> Stream.of(t.getFromProductId(), t.getToProductId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
    
    private Set<Long> extractReasonIds(@NonNull List<ProductTransferResponseDTO> transfers) {
        return transfers.stream()
                .map(ProductTransferResponseDTO::getReasonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
    
    private Map<Long, String> loadWarehouseNames(@NonNull Set<Long> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return warehouseRepository.findAllById(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName, (v1, _) -> v1));
    }
    
    private Map<Long, String> loadProductNames(@NonNull Set<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return StreamSupport.stream(productRepository.findAllById(productIds).spliterator(), false)
                .collect(Collectors.toMap(Product::getId, Product::getName, (v1, _) -> v1));
    }
    
    private Map<Long, String> loadReasonNames(@NonNull Set<Long> reasonIds) {
        if (reasonIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return withdrawalReasonRepository.findAllById(reasonIds).stream()
                .collect(Collectors.toMap(WithdrawalReason::getId, WithdrawalReason::getName, (v1, _) -> v1));
    }
    
    private TransferExcelData buildTransferExcelData(
            @NonNull ProductTransferResponseDTO transfer,
            @NonNull Map<Long, String> warehouseNames,
            @NonNull Map<Long, String> productNames,
            @NonNull Map<Long, String> reasonNames) {
        
        String warehouseName = warehouseNames.getOrDefault(transfer.getWarehouseId(), UNKNOWN_WAREHOUSE);
        String fromProductName = productNames.getOrDefault(transfer.getFromProductId(), UNKNOWN_PRODUCT);
        String toProductName = productNames.getOrDefault(transfer.getToProductId(), UNKNOWN_PRODUCT);
        String reasonName = transfer.getReasonId() != null
                ? reasonNames.getOrDefault(transfer.getReasonId(), UNKNOWN_REASON)
                : UNKNOWN_REASON;
        
        return new TransferExcelData(transfer, warehouseName, fromProductName, toProductName, reasonName);
    }
}
