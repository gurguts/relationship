package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.clients.TransactionApiClient;
import org.example.purchaseservice.repositories.VehicleProductRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.services.impl.IWarehouseProductBalanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import feign.FeignException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleDeletionService {
    
    private final VehicleRepository vehicleRepository;
    private final VehicleProductRepository vehicleProductRepository;
    private final TransactionApiClient transactionApiClient;
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    private final VehicleCostCalculator costCalculator;
    
    private record AggregatedProduct(Long warehouseId, Long productId, BigDecimal quantity, BigDecimal totalCostEur) {}
    private record WarehouseProductKey(Long warehouseId, Long productId) {}
    
    @Transactional
    public void deleteVehicle(@NonNull Long vehicleId) {
        log.info("Deleting vehicle: id={}", vehicleId);
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_NOT_FOUND",
                        String.format("Vehicle not found: id=%d", vehicleId)));

        deleteTransactionsForVehicle(vehicleId);

        List<VehicleProduct> products = vehicleProductRepository.findByVehicleId(vehicleId);

        if (!products.isEmpty()) {
            returnProductsToWarehouse(vehicle, products);
        }

        vehicleProductRepository.deleteAll(products);
        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted and products returned to warehouse: id={}", vehicleId);
    }
    
    private void deleteTransactionsForVehicle(Long vehicleId) {
        try {
            transactionApiClient.deleteTransactionsByVehicleId(vehicleId);
        } catch (FeignException e) {
            log.error("Failed to delete transactions for vehicle: id={}, status={}, error: {}", 
                    vehicleId, e.status(), e.getMessage());
            throw new PurchaseException("FAILED_TO_DELETE_TRANSACTIONS",
                    String.format("Failed to delete transactions for vehicle: %s", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Unexpected error while deleting transactions for vehicle: id={}, error: {}", 
                    vehicleId, e.getMessage(), e);
            throw new PurchaseException("FAILED_TO_DELETE_TRANSACTIONS",
                    String.format("Failed to delete transactions for vehicle: %s", e.getMessage()));
        }
    }
    
    private void returnProductsToWarehouse(Vehicle vehicle, List<VehicleProduct> products) {
        if (products.isEmpty()) {
            return;
        }
        
        Map<WarehouseProductKey, AggregatedProduct> groupedProducts = groupProductsByWarehouseAndProduct(products);
        BigDecimal totalCostToSubtract = returnGroupedProductsToWarehouse(groupedProducts);
        
        if (totalCostToSubtract.compareTo(BigDecimal.ZERO) > 0) {
            costCalculator.subtractVehicleTotalCost(vehicle, totalCostToSubtract);
        }
    }
    
    private Map<WarehouseProductKey, AggregatedProduct> groupProductsByWarehouseAndProduct(List<VehicleProduct> products) {
        return products.stream()
                .collect(Collectors.toMap(
                        p -> new WarehouseProductKey(p.getWarehouseId(), p.getProductId()),
                        p -> new AggregatedProduct(
                                p.getWarehouseId(),
                                p.getProductId(),
                                p.getQuantity() != null ? p.getQuantity() : BigDecimal.ZERO,
                                p.getTotalCostEur() != null ? p.getTotalCostEur() : BigDecimal.ZERO
                        ),
                        (existing, replacement) -> new AggregatedProduct(
                                existing.warehouseId(),
                                existing.productId(),
                                existing.quantity().add(replacement.quantity()),
                                existing.totalCostEur().add(replacement.totalCostEur())
                        )
                ));
    }
    
    private BigDecimal returnGroupedProductsToWarehouse(Map<WarehouseProductKey, AggregatedProduct> groupedProducts) {
        BigDecimal totalCostToSubtract = BigDecimal.ZERO;
        
        for (AggregatedProduct product : groupedProducts.values()) {
            warehouseProductBalanceService.addProduct(
                    product.warehouseId(),
                    product.productId(),
                    product.quantity(),
                    product.totalCostEur()
            );
            
            totalCostToSubtract = totalCostToSubtract.add(product.totalCostEur());
        }
        
        return totalCostToSubtract;
    }
}
