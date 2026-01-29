package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.repositories.VehicleProductRepository;
import org.example.purchaseservice.services.impl.IWarehouseProductBalanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleProductService {
    
    private final VehicleProductRepository vehicleProductRepository;
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    private final VehicleValidator validator;
    private final VehicleCostCalculator costCalculator;
    
    private static final int PRICE_SCALE = 6;
    private static final int QUANTITY_SCALE = 2;
    private static final java.math.RoundingMode PRICE_ROUNDING_MODE = java.math.RoundingMode.HALF_UP;
    
    @Transactional
    public Vehicle updateVehicleProduct(@NonNull Long vehicleId, @NonNull Long vehicleProductId,
                                       BigDecimal newQuantity, BigDecimal newTotalCost) {
        validator.validateUpdateRequest(newQuantity, newTotalCost);
        
        Vehicle vehicle = validator.validateAndGetVehicle(vehicleId);
        VehicleProduct item = validator.validateAndGetVehicleProduct(vehicleId, vehicleProductId);
        
        BigDecimal oldQuantity = item.getQuantity();
        BigDecimal oldTotalCost = item.getTotalCostEur();
        BigDecimal unitPrice = getOrCalculateUnitPrice(item, oldTotalCost, oldQuantity);
        
        boolean itemDeleted = false;
        if (newQuantity != null) {
            itemDeleted = updateVehicleProductQuantity(vehicle, item, newQuantity, oldQuantity, oldTotalCost, unitPrice);
        } else {
            updateVehicleProductTotalCost(vehicle, item, newTotalCost, oldTotalCost, oldQuantity);
        }
        
        if (!itemDeleted) {
            vehicleProductRepository.save(item);
        }
        
        return vehicle;
    }
    
    @Transactional
    public Vehicle addProductToVehicle(@NonNull Long vehicleId, @NonNull Long warehouseId, 
                                      @NonNull Long productId, @NonNull BigDecimal quantity, Long userId) {
        log.info("Adding product to vehicle: vehicleId={}, warehouseId={}, productId={}, quantity={}", 
                vehicleId, warehouseId, productId, quantity);

        validator.validateQuantity(quantity);

        Vehicle vehicle = validator.validateAndGetVehicle(vehicleId);
        WarehouseProductBalance balance = validateAndGetBalance(warehouseId, productId, quantity);
        
        BigDecimal averagePriceEur = balance.getAveragePriceEur() != null 
                ? balance.getAveragePriceEur() 
                : BigDecimal.ZERO;
        
        quantity = quantity.setScale(QUANTITY_SCALE, PRICE_ROUNDING_MODE);
        BigDecimal totalCost = costCalculator.calculateTotalCost(quantity, averagePriceEur);
        
        warehouseProductBalanceService.removeProductWithCost(warehouseId, productId, quantity, totalCost);
        
        VehicleProduct vehicleProduct = createVehicleProduct(vehicleId, warehouseId, productId, 
                quantity, averagePriceEur, totalCost, userId);
        vehicleProductRepository.save(vehicleProduct);
        
        costCalculator.addVehicleTotalCost(vehicle, totalCost);
        
        log.info("Product added to vehicle: vehicleId={}, totalCost={}, averagePrice={}", 
                vehicle.getId(), totalCost, averagePriceEur);
        
        return vehicle;
    }
    
    private BigDecimal getOrCalculateUnitPrice(VehicleProduct item, BigDecimal oldTotalCost, BigDecimal oldQuantity) {
        BigDecimal unitPrice = item.getUnitPriceEur();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            unitPrice = costCalculator.calculateUnitPrice(oldTotalCost, oldQuantity);
        }
        return unitPrice;
    }
    
    private boolean updateVehicleProductQuantity(Vehicle vehicle, VehicleProduct item, BigDecimal newQuantity,
                                                 BigDecimal oldQuantity, BigDecimal oldTotalCost, BigDecimal unitPrice) {
        newQuantity = newQuantity.setScale(QUANTITY_SCALE, PRICE_ROUNDING_MODE);
        validator.validateQuantityNonNegative(newQuantity);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            removeVehicleProductItem(vehicle, item, oldQuantity, oldTotalCost);
            return true;
        }
        
        validator.validateNoChanges(oldQuantity, newQuantity, "Quantity");
        
        BigDecimal deltaQuantity = newQuantity.subtract(oldQuantity);
        BigDecimal deltaCost = deltaQuantity.multiply(unitPrice).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
        
        if (deltaQuantity.compareTo(BigDecimal.ZERO) > 0) {
            handleQuantityIncrease(vehicle, item, deltaQuantity, deltaCost);
        } else {
            handleQuantityDecrease(vehicle, item, deltaQuantity, deltaCost);
        }
        
        BigDecimal newTotal = oldTotalCost.add(deltaCost).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
        if (newTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_TOTAL_COST", "Resulting total cost must be positive");
        }
        
        item.setQuantity(newQuantity);
        item.setTotalCostEur(newTotal);
        item.setUnitPriceEur(costCalculator.calculateUnitPrice(newTotal, newQuantity));
        return false;
    }
    
    private void removeVehicleProductItem(Vehicle vehicle, VehicleProduct item, 
                                         BigDecimal oldQuantity, BigDecimal oldTotalCost) {
        warehouseProductBalanceService.addProduct(
                item.getWarehouseId(),
                item.getProductId(),
                oldQuantity,
                oldTotalCost
        );
        
        if (oldTotalCost.compareTo(BigDecimal.ZERO) > 0) {
            costCalculator.subtractVehicleTotalCost(vehicle, oldTotalCost);
        }
        
        vehicleProductRepository.delete(item);
    }
    
    private void handleQuantityIncrease(Vehicle vehicle, VehicleProduct item, 
                                       BigDecimal deltaQuantity, BigDecimal deltaCost) {
        WarehouseProductBalance balance =
                warehouseProductBalanceService.getBalance(item.getWarehouseId(), item.getProductId());
        
        if (balance == null) {
            throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                    "Insufficient product on warehouse. Available: 0, requested additionally: %s",
                    deltaQuantity));
        }
        
        BigDecimal availableQuantity = balance.getQuantity();
        if (availableQuantity == null || availableQuantity.compareTo(deltaQuantity) < 0) {
            String availableStr = availableQuantity != null ? availableQuantity.toString() : "0";
            throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                    "Insufficient product on warehouse. Available: %s, requested additionally: %s",
                    availableStr, deltaQuantity));
        }
        
        warehouseProductBalanceService.removeProductWithCost(
                item.getWarehouseId(), item.getProductId(), deltaQuantity, deltaCost);
        costCalculator.addVehicleTotalCost(vehicle, deltaCost);
    }
    
    private void handleQuantityDecrease(Vehicle vehicle, VehicleProduct item, 
                                        BigDecimal deltaQuantity, BigDecimal deltaCost) {
        BigDecimal returnQuantity = deltaQuantity.abs();
        BigDecimal returnCost = deltaCost.abs();
        
        warehouseProductBalanceService.addProduct(
                item.getWarehouseId(), item.getProductId(), returnQuantity, returnCost);
        costCalculator.subtractVehicleTotalCost(vehicle, returnCost);
    }
    
    private void updateVehicleProductTotalCost(Vehicle vehicle, VehicleProduct item, 
                                              BigDecimal newTotalCost, BigDecimal oldTotalCost, BigDecimal oldQuantity) {
        validator.validateTotalCost(newTotalCost);
        validator.validateNoChanges(oldTotalCost, newTotalCost, "Total cost");
        
        newTotalCost = newTotalCost.setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
        BigDecimal deltaCost = newTotalCost.subtract(oldTotalCost);
        
        warehouseProductBalanceService.adjustProductCost(
                item.getWarehouseId(), item.getProductId(), deltaCost.negate());
        
        if (deltaCost.compareTo(BigDecimal.ZERO) > 0) {
            costCalculator.addVehicleTotalCost(vehicle, deltaCost);
        } else {
            costCalculator.subtractVehicleTotalCost(vehicle, deltaCost.abs());
        }
        
        item.setTotalCostEur(newTotalCost);
        item.setUnitPriceEur(costCalculator.calculateUnitPrice(newTotalCost, oldQuantity));
    }
    
    private WarehouseProductBalance validateAndGetBalance(
            Long warehouseId, Long productId, BigDecimal quantity) {
        WarehouseProductBalance balance =
                warehouseProductBalanceService.getBalance(warehouseId, productId);
        
        if (balance == null) {
            throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                    "Insufficient product on warehouse. Available: 0, requested: %s",
                    quantity));
        }
        
        BigDecimal availableQuantity = balance.getQuantity();
        if (availableQuantity == null || availableQuantity.compareTo(quantity) < 0) {
            String availableStr = availableQuantity != null ? availableQuantity.toString() : "0";
            throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                    "Insufficient product on warehouse. Available: %s, requested: %s",
                    availableStr, quantity));
        }
        
        return balance;
    }
    
    private VehicleProduct createVehicleProduct(Long vehicleId, Long warehouseId, Long productId,
                                                BigDecimal quantity, BigDecimal averagePrice, 
                                                BigDecimal totalCost, Long userId) {
        VehicleProduct vehicleProduct = new VehicleProduct();
        vehicleProduct.setVehicleId(vehicleId);
        vehicleProduct.setWarehouseId(warehouseId);
        vehicleProduct.setProductId(productId);
        vehicleProduct.setQuantity(quantity);
        vehicleProduct.setUnitPriceEur(averagePrice);
        vehicleProduct.setTotalCostEur(totalCost);
        vehicleProduct.setUserId(userId);
        return vehicleProduct;
    }
}
