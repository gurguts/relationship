package org.example.purchaseservice.services.vehicle;

import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.repositories.VehicleProductRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class VehicleValidator {
    
    private final VehicleRepository vehicleRepository;
    private final VehicleProductRepository vehicleProductRepository;
    
    public Vehicle validateAndGetVehicle(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_NOT_FOUND",
                        String.format("Vehicle not found: id=%d", vehicleId)));
    }
    
    public void validateUpdateRequest(BigDecimal newQuantity, BigDecimal newTotalCost) {
        if ((newQuantity == null && newTotalCost == null) ||
                (newQuantity != null && newTotalCost != null)) {
            throw new PurchaseException("INVALID_UPDATE_REQUEST",
                    "Specify either quantity or total cost to update, but not both");
        }
    }
    
    public VehicleProduct validateAndGetVehicleProduct(Long vehicleId, Long vehicleProductId) {
        VehicleProduct item = vehicleProductRepository.findById(vehicleProductId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_PRODUCT_NOT_FOUND",
                        String.format("Vehicle product not found: id=%d", vehicleProductId)));
        
        if (!item.getVehicleId().equals(vehicleId)) {
            throw new PurchaseException("VEHICLE_PRODUCT_MISMATCH",
                    "Product does not belong to the specified vehicle");
        }
        
        BigDecimal oldQuantity = item.getQuantity();
        if (oldQuantity == null || oldQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_ITEM_STATE", "Vehicle item quantity is invalid");
        }
        
        return item;
    }
    
    public void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity must be greater than zero");
        }
    }
    
    public void validateQuantityNonNegative(BigDecimal quantity) {
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be negative");
        }
    }
    
    public void validateTotalCost(BigDecimal totalCost) {
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_TOTAL_COST", "Total cost must be greater than zero");
        }
    }
    
    public void validateNoChanges(BigDecimal oldValue, BigDecimal newValue, String fieldName) {
        if (oldValue != null && newValue != null && oldValue.compareTo(newValue) == 0) {
            throw new PurchaseException("NO_CHANGES", String.format("%s has not changed", fieldName));
        }
    }
}
