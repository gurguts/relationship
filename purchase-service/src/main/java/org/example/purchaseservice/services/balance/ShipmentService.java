package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.Shipment;
import org.example.purchaseservice.models.balance.ShipmentProduct;
import org.example.purchaseservice.models.dto.balance.ShipmentUpdateDTO;
import org.example.purchaseservice.repositories.ShipmentRepository;
import org.example.purchaseservice.repositories.ShipmentProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {
    
    private final ShipmentRepository shipmentRepository;
    private final ShipmentProductRepository shipmentProductRepository;
    private final WarehouseProductBalanceService warehouseProductBalanceService;

    @Transactional
    public Shipment createShipment(LocalDate shipmentDate, String vehicleNumber,
                                    String invoiceUa, String invoiceEu,
                                    String description, Long userId) {
        log.info("Creating new shipment: date={}, vehicle={}, invoiceUa={}, invoiceEu={}", 
                shipmentDate, vehicleNumber, invoiceUa, invoiceEu);
        
        Shipment shipment = new Shipment();
        shipment.setShipmentDate(shipmentDate);
        shipment.setVehicleNumber(vehicleNumber);
        shipment.setInvoiceUa(invoiceUa);
        shipment.setInvoiceEu(invoiceEu);
        shipment.setDescription(description);
        shipment.setUserId(userId);
        shipment.setTotalCostEur(BigDecimal.ZERO);
        
        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipment created: id={}", saved.getId());
        
        return saved;
    }

    @Transactional
    public Shipment addWithdrawalCost(Long shipmentId, BigDecimal withdrawalCost) {
        log.info("Adding withdrawal cost to shipment: shipmentId={}, cost={}", shipmentId, withdrawalCost);
        
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new PurchaseException("SHIPMENT_NOT_FOUND", 
                        String.format("Shipment not found: id=%d", shipmentId)));
        
        shipment.addWithdrawalCost(withdrawalCost);
        Shipment saved = shipmentRepository.save(shipment);
        
        log.info("Shipment updated: id={}, newTotalCost={}", saved.getId(), saved.getTotalCostEur());
        
        return saved;
    }

    @Transactional
    public Shipment subtractWithdrawalCost(Long shipmentId, BigDecimal withdrawalCost) {
        log.info("Subtracting withdrawal cost from shipment: shipmentId={}, cost={}", shipmentId, withdrawalCost);
        
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new PurchaseException("SHIPMENT_NOT_FOUND", 
                        String.format("Shipment not found: id=%d", shipmentId)));
        
        shipment.subtractWithdrawalCost(withdrawalCost);
        Shipment saved = shipmentRepository.save(shipment);
        
        log.info("Shipment updated: id={}, newTotalCost={}", saved.getId(), saved.getTotalCostEur());
        
        return saved;
    }

    public Shipment getShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId).orElse(null);
    }

    public List<Shipment> getShipmentsByDate(LocalDate date) {
        return shipmentRepository.findByShipmentDate(date);
    }

    public List<Shipment> getShipmentsByDateRange(LocalDate fromDate, LocalDate toDate) {
        return shipmentRepository.findByDateRange(fromDate, toDate);
    }

    public List<Shipment> getUserShipments(Long userId) {
        return shipmentRepository.findByUserId(userId);
    }
    
    @Transactional
    public Shipment updateShipmentProduct(Long shipmentId, Long shipmentProductId,
                                          BigDecimal newQuantity, BigDecimal newTotalCost) {
        if ((newQuantity == null && newTotalCost == null) ||
                (newQuantity != null && newTotalCost != null)) {
            throw new PurchaseException("INVALID_UPDATE_REQUEST",
                    "Specify either quantity or total cost to update, but not both");
        }

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new PurchaseException("SHIPMENT_NOT_FOUND",
                        String.format("Shipment not found: id=%d", shipmentId)));

        ShipmentProduct item = shipmentProductRepository.findById(shipmentProductId)
                .orElseThrow(() -> new PurchaseException("SHIPMENT_PRODUCT_NOT_FOUND",
                        String.format("Shipment product not found: id=%d", shipmentProductId)));

        if (!item.getShipmentId().equals(shipmentId)) {
            throw new PurchaseException("SHIPMENT_PRODUCT_MISMATCH",
                    "Product does not belong to the specified shipment");
        }

        BigDecimal oldQuantity = item.getQuantity();
        BigDecimal oldTotalCost = item.getTotalCostEur();
        BigDecimal unitPrice = item.getUnitPriceEur();

        if (oldQuantity == null || oldQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_ITEM_STATE", "Shipment item quantity is invalid");
        }

        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            unitPrice = oldTotalCost.divide(oldQuantity, 6, java.math.RoundingMode.HALF_UP);
        }

        if (newQuantity != null) {
            newQuantity = newQuantity.setScale(2, java.math.RoundingMode.HALF_UP);
            if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be negative");
            }

            if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
                // Remove item from shipment, return product to warehouse
                warehouseProductBalanceService.addProduct(
                        item.getWarehouseId(),
                        item.getProductId(),
                        oldQuantity,
                        oldTotalCost
                );

                shipment.subtractWithdrawalCost(oldTotalCost);
                shipmentProductRepository.delete(item);
                shipmentRepository.save(shipment);
                return shipment;
            }

            BigDecimal deltaQuantity = newQuantity.subtract(oldQuantity);
            if (deltaQuantity.compareTo(BigDecimal.ZERO) == 0) {
                throw new PurchaseException("NO_CHANGES", "Quantity has not changed");
            }

            BigDecimal deltaCost = deltaQuantity.multiply(unitPrice).setScale(6, java.math.RoundingMode.HALF_UP);

            if (deltaQuantity.compareTo(BigDecimal.ZERO) > 0) {
                // Need to remove additional product from warehouse
                if (!warehouseProductBalanceService.hasEnoughProduct(item.getWarehouseId(), item.getProductId(), deltaQuantity)) {
                    org.example.purchaseservice.models.balance.WarehouseProductBalance balance =
                            warehouseProductBalanceService.getBalance(item.getWarehouseId(), item.getProductId());

                    throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                            "Insufficient product on warehouse. Available: %s, requested additionally: %s",
                            balance != null ? balance.getQuantity() : BigDecimal.ZERO,
                            deltaQuantity));
                }

                warehouseProductBalanceService.removeProductWithCost(
                        item.getWarehouseId(), item.getProductId(), deltaQuantity, deltaCost);
                shipment.addWithdrawalCost(deltaCost);
            } else {
                // Return product to warehouse when quantity decreased
                BigDecimal returnQuantity = deltaQuantity.abs();
                BigDecimal returnCost = deltaCost.abs();

                warehouseProductBalanceService.addProduct(
                        item.getWarehouseId(), item.getProductId(), returnQuantity, returnCost);
                shipment.subtractWithdrawalCost(returnCost);
            }

            BigDecimal newTotal = oldTotalCost.add(deltaCost).setScale(6, java.math.RoundingMode.HALF_UP);
            if (newTotal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new PurchaseException("INVALID_TOTAL_COST", "Resulting total cost must be positive");
            }

            item.setQuantity(newQuantity);
            item.setTotalCostEur(newTotal);
            item.setUnitPriceEur(newTotal.divide(newQuantity, 6, java.math.RoundingMode.HALF_UP));
        } else {
            if (newTotalCost == null || newTotalCost.compareTo(BigDecimal.ZERO) <= 0) {
                throw new PurchaseException("INVALID_TOTAL_COST", "Total cost must be greater than zero");
            }
            newTotalCost = newTotalCost.setScale(6, java.math.RoundingMode.HALF_UP);
            BigDecimal deltaCost = newTotalCost.subtract(oldTotalCost);
            if (deltaCost.compareTo(BigDecimal.ZERO) == 0) {
                throw new PurchaseException("NO_CHANGES", "Total cost has not changed");
            }

            // Adjust warehouse cost without changing quantity
            warehouseProductBalanceService.adjustProductCost(
                    item.getWarehouseId(), item.getProductId(), deltaCost.negate());

            if (deltaCost.compareTo(BigDecimal.ZERO) > 0) {
                shipment.addWithdrawalCost(deltaCost);
            } else {
                shipment.subtractWithdrawalCost(deltaCost.abs());
            }

            item.setTotalCostEur(newTotalCost);
            item.setUnitPriceEur(newTotalCost.divide(oldQuantity, 6, java.math.RoundingMode.HALF_UP));
        }

        shipmentProductRepository.save(item);
        shipmentRepository.save(shipment);

        return shipment;
    }

    @Transactional
    public Shipment updateShipment(Long shipmentId, ShipmentUpdateDTO dto) {
        log.info("Updating shipment: id={}", shipmentId);
        
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new PurchaseException("SHIPMENT_NOT_FOUND",
                        String.format("Shipment not found: id=%d", shipmentId)));
        
        if (dto.getShipmentDate() != null) {
            shipment.setShipmentDate(dto.getShipmentDate());
        }
        
        shipment.setVehicleNumber(normalizeString(dto.getVehicleNumber()));
        shipment.setInvoiceUa(normalizeString(dto.getInvoiceUa()));
        shipment.setInvoiceEu(normalizeString(dto.getInvoiceEu()));
        shipment.setDescription(normalizeString(dto.getDescription()));
        
        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipment updated: id={}", saved.getId());
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
    public ShipmentProduct addProductToShipment(Long shipmentId, Long warehouseId, 
                                                 Long productId, BigDecimal quantity, Long userId) {
        log.info("Adding product to shipment: shipmentId={}, warehouseId={}, productId={}, quantity={}", 
                shipmentId, warehouseId, productId, quantity);

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new PurchaseException("SHIPMENT_NOT_FOUND", 
                        String.format("Shipment not found: id=%d", shipmentId)));

        if (!warehouseProductBalanceService.hasEnoughProduct(warehouseId, productId, quantity)) {
            org.example.purchaseservice.models.balance.WarehouseProductBalance balance = 
                    warehouseProductBalanceService.getBalance(warehouseId, productId);
            
            throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                    "Insufficient product on warehouse. Available: %s, requested: %s",
                    balance != null ? balance.getQuantity() : BigDecimal.ZERO,
                    quantity));
        }

        BigDecimal averagePrice = warehouseProductBalanceService.removeProduct(warehouseId, productId, quantity);

        BigDecimal totalCost = quantity.multiply(averagePrice);

        ShipmentProduct shipmentProduct = new ShipmentProduct();
        shipmentProduct.setShipmentId(shipmentId);
        shipmentProduct.setWarehouseId(warehouseId);
        shipmentProduct.setProductId(productId);
        shipmentProduct.setQuantity(quantity);
        shipmentProduct.setUnitPriceEur(averagePrice);
        shipmentProduct.setTotalCostEur(totalCost);
        shipmentProduct.setUserId(userId);
        
        ShipmentProduct saved = shipmentProductRepository.save(shipmentProduct);

        shipment.addWithdrawalCost(totalCost);
        shipmentRepository.save(shipment);
        
        log.info("Product added to shipment: id={}, totalCost={}", saved.getId(), totalCost);
        
        return saved;
    }

    public List<ShipmentProduct> getShipmentProducts(Long shipmentId) {
        return shipmentProductRepository.findByShipmentId(shipmentId);
    }

    @Transactional
    public void deleteShipment(Long shipmentId) {
        log.info("Deleting shipment: id={}", shipmentId);
        
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new PurchaseException("SHIPMENT_NOT_FOUND",
                        String.format("Shipment not found: id=%d", shipmentId)));

        List<ShipmentProduct> products = shipmentProductRepository.findByShipmentId(shipmentId);

        for (ShipmentProduct product : products) {
            warehouseProductBalanceService.addProduct(
                    product.getWarehouseId(),
                    product.getProductId(),
                    product.getQuantity(),
                    product.getTotalCostEur()
            );

            shipment.subtractWithdrawalCost(product.getTotalCostEur());
        }

        shipmentProductRepository.deleteAll(products);
        shipmentRepository.delete(shipment);
        log.info("Shipment deleted and products returned to warehouse: id={}", shipmentId);
    }
}

