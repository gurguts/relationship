package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.models.dto.balance.VehicleUpdateDTO;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.repositories.VehicleProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {
    
    private final VehicleRepository vehicleRepository;
    private final VehicleProductRepository vehicleProductRepository;
    private final WarehouseProductBalanceService warehouseProductBalanceService;

    @Transactional
    public Vehicle createVehicle(LocalDate shipmentDate, String vehicleNumber,
                                    String invoiceUa, String invoiceEu,
                                    String description, Long userId, Boolean isOurVehicle) {
        log.info("Creating new vehicle: date={}, vehicle={}, invoiceUa={}, invoiceEu={}, isOurVehicle={}", 
                shipmentDate, vehicleNumber, invoiceUa, invoiceEu, isOurVehicle);
        
        Vehicle vehicle = new Vehicle();
        vehicle.setShipmentDate(shipmentDate);
        vehicle.setVehicleNumber(vehicleNumber);
        vehicle.setInvoiceUa(invoiceUa);
        vehicle.setInvoiceEu(invoiceEu);
        vehicle.setDescription(description);
        vehicle.setUserId(userId);
        vehicle.setIsOurVehicle(isOurVehicle != null ? isOurVehicle : true);
        vehicle.setTotalCostEur(BigDecimal.ZERO);
        
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle created: id={}", saved.getId());
        
        return saved;
    }

    @Transactional
    public Vehicle addWithdrawalCost(Long vehicleId, BigDecimal withdrawalCost) {
        log.info("Adding withdrawal cost to vehicle: vehicleId={}, cost={}", vehicleId, withdrawalCost);
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_NOT_FOUND", 
                        String.format("Vehicle not found: id=%d", vehicleId)));
        
        vehicle.addWithdrawalCost(withdrawalCost);
        Vehicle saved = vehicleRepository.save(vehicle);
        
        log.info("Vehicle updated: id={}, newTotalCost={}", saved.getId(), saved.getTotalCostEur());
        
        return saved;
    }

    @Transactional
    public Vehicle subtractWithdrawalCost(Long vehicleId, BigDecimal withdrawalCost) {
        log.info("Subtracting withdrawal cost from vehicle: vehicleId={}, cost={}", vehicleId, withdrawalCost);
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_NOT_FOUND", 
                        String.format("Vehicle not found: id=%d", vehicleId)));
        
        vehicle.subtractWithdrawalCost(withdrawalCost);
        Vehicle saved = vehicleRepository.save(vehicle);
        
        log.info("Vehicle updated: id={}, newTotalCost={}", saved.getId(), saved.getTotalCostEur());
        
        return saved;
    }

    public Vehicle getVehicle(Long vehicleId) {
        return vehicleRepository.findById(vehicleId).orElse(null);
    }

    public List<Vehicle> getVehiclesByDate(LocalDate date) {
        return vehicleRepository.findByShipmentDate(date);
    }

    public List<Vehicle> getVehiclesByDateRange(LocalDate fromDate, LocalDate toDate) {
        return vehicleRepository.findByDateRange(fromDate, toDate);
    }
    
    public List<Vehicle> getOurVehiclesByDateRange(LocalDate fromDate, LocalDate toDate) {
        return vehicleRepository.findOurVehiclesByDateRange(fromDate, toDate);
    }
    
    public List<Vehicle> getAllVehiclesByDateRange(LocalDate fromDate, LocalDate toDate) {
        return vehicleRepository.findAllVehiclesByDateRange(fromDate, toDate);
    }

    public List<Vehicle> getUserVehicles(Long userId) {
        return vehicleRepository.findByUserId(userId);
    }
    
    @Transactional
    public Vehicle updateVehicleProduct(Long vehicleId, Long vehicleProductId,
                                          BigDecimal newQuantity, BigDecimal newTotalCost) {
        if ((newQuantity == null && newTotalCost == null) ||
                (newQuantity != null && newTotalCost != null)) {
            throw new PurchaseException("INVALID_UPDATE_REQUEST",
                    "Specify either quantity or total cost to update, but not both");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_NOT_FOUND",
                        String.format("Vehicle not found: id=%d", vehicleId)));

        VehicleProduct item = vehicleProductRepository.findById(vehicleProductId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_PRODUCT_NOT_FOUND",
                        String.format("Vehicle product not found: id=%d", vehicleProductId)));

        if (!item.getVehicleId().equals(vehicleId)) {
            throw new PurchaseException("VEHICLE_PRODUCT_MISMATCH",
                    "Product does not belong to the specified vehicle");
        }

        BigDecimal oldQuantity = item.getQuantity();
        BigDecimal oldTotalCost = item.getTotalCostEur();
        BigDecimal unitPrice = item.getUnitPriceEur();

        if (oldQuantity == null || oldQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_ITEM_STATE", "Vehicle item quantity is invalid");
        }

        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            unitPrice = oldTotalCost.divide(oldQuantity, 6, java.math.RoundingMode.CEILING);
        }

        if (newQuantity != null) {
            newQuantity = newQuantity.setScale(2, java.math.RoundingMode.HALF_UP);
            if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be negative");
            }

            if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
                warehouseProductBalanceService.addProduct(
                        item.getWarehouseId(),
                        item.getProductId(),
                        oldQuantity,
                        oldTotalCost
                );

                vehicle.subtractWithdrawalCost(oldTotalCost);
                vehicleProductRepository.delete(item);
                vehicleRepository.save(vehicle);
                return vehicle;
            }

            BigDecimal deltaQuantity = newQuantity.subtract(oldQuantity);
            if (deltaQuantity.compareTo(BigDecimal.ZERO) == 0) {
                throw new PurchaseException("NO_CHANGES", "Quantity has not changed");
            }

            BigDecimal deltaCost = deltaQuantity.multiply(unitPrice).setScale(6, java.math.RoundingMode.HALF_UP);

            if (deltaQuantity.compareTo(BigDecimal.ZERO) > 0) {
                org.example.purchaseservice.models.balance.WarehouseProductBalance balance =
                        warehouseProductBalanceService.getBalance(item.getWarehouseId(), item.getProductId());

                if (balance == null) {
                    throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                            "Insufficient product on warehouse. Available: 0, requested additionally: %s",
                            deltaQuantity));
                }

                BigDecimal availableQuantity = balance.getQuantity();

                if (availableQuantity.compareTo(deltaQuantity) < 0) {
                    throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                            "Insufficient product on warehouse. Available: %s, requested additionally: %s",
                            availableQuantity, deltaQuantity));
                }

                warehouseProductBalanceService.removeProductWithCost(
                        item.getWarehouseId(), item.getProductId(), deltaQuantity, deltaCost);
                vehicle.addWithdrawalCost(deltaCost);
            } else {
                BigDecimal returnQuantity = deltaQuantity.abs();
                BigDecimal returnCost = deltaCost.abs();

                warehouseProductBalanceService.addProduct(
                        item.getWarehouseId(), item.getProductId(), returnQuantity, returnCost);
                vehicle.subtractWithdrawalCost(returnCost);
            }

            BigDecimal newTotal = oldTotalCost.add(deltaCost).setScale(6, java.math.RoundingMode.HALF_UP);
            if (newTotal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new PurchaseException("INVALID_TOTAL_COST", "Resulting total cost must be positive");
            }

            item.setQuantity(newQuantity);
            item.setTotalCostEur(newTotal);
            item.setUnitPriceEur(newTotal.divide(newQuantity, 6, java.math.RoundingMode.CEILING));
        } else {
            if (newTotalCost == null || newTotalCost.compareTo(BigDecimal.ZERO) <= 0) {
                throw new PurchaseException("INVALID_TOTAL_COST", "Total cost must be greater than zero");
            }
            newTotalCost = newTotalCost.setScale(6, java.math.RoundingMode.HALF_UP);
            BigDecimal deltaCost = newTotalCost.subtract(oldTotalCost);
            if (deltaCost.compareTo(BigDecimal.ZERO) == 0) {
                throw new PurchaseException("NO_CHANGES", "Total cost has not changed");
            }

            warehouseProductBalanceService.adjustProductCost(
                    item.getWarehouseId(), item.getProductId(), deltaCost.negate());

            if (deltaCost.compareTo(BigDecimal.ZERO) > 0) {
                vehicle.addWithdrawalCost(deltaCost);
            } else {
                vehicle.subtractWithdrawalCost(deltaCost.abs());
            }

            item.setTotalCostEur(newTotalCost);
            item.setUnitPriceEur(newTotalCost.divide(oldQuantity, 6, java.math.RoundingMode.CEILING));
        }

        vehicleProductRepository.save(item);
        vehicleRepository.save(vehicle);

        return vehicle;
    }

    @Transactional
    public Vehicle updateVehicle(Long vehicleId, VehicleUpdateDTO dto) {
        log.info("Updating vehicle: id={}", vehicleId);
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_NOT_FOUND",
                        String.format("Vehicle not found: id=%d", vehicleId)));
        
        if (dto.getShipmentDate() != null) {
            vehicle.setShipmentDate(dto.getShipmentDate());
        }
        
        vehicle.setVehicleNumber(normalizeString(dto.getVehicleNumber()));
        vehicle.setInvoiceUa(normalizeString(dto.getInvoiceUa()));
        vehicle.setInvoiceEu(normalizeString(dto.getInvoiceEu()));
        vehicle.setDescription(normalizeString(dto.getDescription()));
        
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: id={}", saved.getId());
        return saved;
    }
    
    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public VehicleProduct addProductToVehicle(Long vehicleId, Long warehouseId, 
                                                 Long productId, BigDecimal quantity, Long userId) {
        log.info("Adding product to vehicle: vehicleId={}, warehouseId={}, productId={}, quantity={}", 
                vehicleId, warehouseId, productId, quantity);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_NOT_FOUND", 
                        String.format("Vehicle not found: id=%d", vehicleId)));

        org.example.purchaseservice.models.balance.WarehouseProductBalance balance = 
                warehouseProductBalanceService.getBalance(warehouseId, productId);

        if (balance == null) {
            throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                    "Insufficient product on warehouse. Available: 0, requested: %s",
                    quantity));
        }

        BigDecimal availableQuantity = balance.getQuantity();

        if (availableQuantity.compareTo(quantity) < 0) {
            throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                    "Insufficient product on warehouse. Available: %s, requested: %s",
                    availableQuantity, quantity));
        }

        quantity = quantity.setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal averagePrice = balance.getAveragePriceEur();
        BigDecimal totalCost = quantity.multiply(averagePrice).setScale(6, java.math.RoundingMode.HALF_UP);

        log.info("Before removeProductWithCost: warehouseId={}, productId={}, quantity={}, averagePrice={}, totalCost={}, currentBalanceQuantity={}, currentBalanceTotalCost={}",
                warehouseId, productId, quantity, averagePrice, totalCost, balance.getQuantity(), balance.getTotalCostEur());

        warehouseProductBalanceService.removeProductWithCost(warehouseId, productId, quantity, totalCost);

        org.example.purchaseservice.models.balance.WarehouseProductBalance updatedBalance = warehouseProductBalanceService.getBalance(warehouseId, productId);
        if (updatedBalance != null) {
            log.info("After removeProductWithCost: warehouseId={}, productId={}, newBalanceQuantity={}, newBalanceTotalCost={}, newAveragePrice={}",
                    warehouseId, productId, updatedBalance.getQuantity(), updatedBalance.getTotalCostEur(), updatedBalance.getAveragePriceEur());
        } else {
            log.info("After removeProductWithCost: warehouseId={}, productId={}, balance deleted (quantity=0)", warehouseId, productId);
        }

        VehicleProduct vehicleProduct = new VehicleProduct();
        vehicleProduct.setVehicleId(vehicleId);
        vehicleProduct.setWarehouseId(warehouseId);
        vehicleProduct.setProductId(productId);
        vehicleProduct.setQuantity(quantity);
        vehicleProduct.setUnitPriceEur(averagePrice);
        vehicleProduct.setTotalCostEur(totalCost);
        vehicleProduct.setUserId(userId);
        
        VehicleProduct saved = vehicleProductRepository.save(vehicleProduct);

        vehicle.addWithdrawalCost(totalCost);
        vehicleRepository.save(vehicle);
        
        log.info("Product added to vehicle: id={}, totalCost={}", saved.getId(), totalCost);
        
        return saved;
    }

    public List<VehicleProduct> getVehicleProducts(Long vehicleId) {
        return vehicleProductRepository.findByVehicleId(vehicleId);
    }

    @Transactional
    public void deleteVehicle(Long vehicleId) {
        log.info("Deleting vehicle: id={}", vehicleId);
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_NOT_FOUND",
                        String.format("Vehicle not found: id=%d", vehicleId)));

        List<VehicleProduct> products = vehicleProductRepository.findByVehicleId(vehicleId);

        for (VehicleProduct product : products) {
            warehouseProductBalanceService.addProduct(
                    product.getWarehouseId(),
                    product.getProductId(),
                    product.getQuantity(),
                    product.getTotalCostEur()
            );

            vehicle.subtractWithdrawalCost(product.getTotalCostEur());
        }

        vehicleProductRepository.deleteAll(products);
        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted and products returned to warehouse: id={}", vehicleId);
    }
}

