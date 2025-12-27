package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.balance.Carrier;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.models.balance.VehicleReceiver;
import org.example.purchaseservice.models.balance.VehicleSender;
import org.example.purchaseservice.models.dto.balance.CarrierDetailsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleUpdateDTO;
import org.example.purchaseservice.clients.TransactionApiClient;
import org.example.purchaseservice.repositories.CarrierRepository;
import org.example.purchaseservice.repositories.VehicleReceiverRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.repositories.VehicleSenderRepository;
import org.example.purchaseservice.repositories.VehicleProductRepository;
import org.example.purchaseservice.spec.VehicleSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {
    
    private final VehicleRepository vehicleRepository;
    private final VehicleProductRepository vehicleProductRepository;
    private final WarehouseProductBalanceService warehouseProductBalanceService;
    private final CarrierRepository carrierRepository;
    private final VehicleSenderRepository vehicleSenderRepository;
    private final VehicleReceiverRepository vehicleReceiverRepository;
    private final TransactionApiClient transactionApiClient;
    private final org.example.purchaseservice.services.balance.VehicleExpenseService vehicleExpenseService;

    @Transactional
    public Vehicle createVehicle(LocalDate shipmentDate, String vehicleNumber,
                                    String invoiceUa, String invoiceEu,
                                    String description, Long userId, Boolean isOurVehicle,
                                    Long senderId, Long receiverId, String destinationCountry,
                                    String destinationPlace, String product, String productQuantity,
                                    String declarationNumber, String terminal, String driverFullName,
                                    Boolean eur1, Boolean fito, LocalDate customsDate,
                                    LocalDate customsClearanceDate, LocalDate unloadingDate,
                                    LocalDate invoiceUaDate, BigDecimal invoiceUaPricePerTon,
                                    LocalDate invoiceEuDate, BigDecimal invoiceEuPricePerTon,
                                    BigDecimal reclamation, Long carrierId) {
        log.info("Creating new vehicle: date={}, vehicle={}, invoiceUa={}, invoiceEu={}, isOurVehicle={}", 
                shipmentDate, vehicleNumber, invoiceUa, invoiceEu, isOurVehicle);
        
        Vehicle vehicle = new Vehicle();
        vehicle.setShipmentDate(shipmentDate != null ? shipmentDate : LocalDate.now());
        vehicle.setVehicleNumber(vehicleNumber);
        vehicle.setInvoiceUa(invoiceUa);
        vehicle.setInvoiceEu(invoiceEu);
        vehicle.setDescription(description);
        vehicle.setUserId(userId);
        vehicle.setIsOurVehicle(isOurVehicle != null ? isOurVehicle : false);
        vehicle.setTotalCostEur(BigDecimal.ZERO);
        
        if (senderId != null) {
            VehicleSender sender = vehicleSenderRepository.findById(senderId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_SENDER_NOT_FOUND",
                            String.format("Vehicle sender not found: id=%d", senderId)));
            vehicle.setSender(sender);
        }
        
        if (receiverId != null) {
            VehicleReceiver receiver = vehicleReceiverRepository.findById(receiverId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_RECEIVER_NOT_FOUND",
                            String.format("Vehicle receiver not found: id=%d", receiverId)));
            vehicle.setReceiver(receiver);
        }
        
        vehicle.setDestinationCountry(normalizeString(destinationCountry));
        vehicle.setDestinationPlace(normalizeString(destinationPlace));
        vehicle.setProduct(normalizeString(product));
        vehicle.setProductQuantity(normalizeString(productQuantity));
        vehicle.setDeclarationNumber(normalizeString(declarationNumber));
        vehicle.setTerminal(normalizeString(terminal));
        vehicle.setDriverFullName(normalizeString(driverFullName));
        vehicle.setEur1(eur1 != null ? eur1 : false);
        vehicle.setFito(fito != null ? fito : false);
        vehicle.setCustomsDate(customsDate);
        vehicle.setCustomsClearanceDate(customsClearanceDate);
        vehicle.setUnloadingDate(unloadingDate);
        vehicle.setInvoiceUaDate(invoiceUaDate);
        vehicle.setInvoiceUaPricePerTon(invoiceUaPricePerTon);
        vehicle.setInvoiceEuDate(invoiceEuDate);
        vehicle.setInvoiceEuPricePerTon(invoiceEuPricePerTon);
        vehicle.setReclamation(reclamation);
        
        if (invoiceUaPricePerTon != null && productQuantity != null) {
            try {
                BigDecimal quantityInTons = new BigDecimal(productQuantity.replace(",", "."));
                vehicle.setInvoiceUaTotalPrice(invoiceUaPricePerTon.multiply(quantityInTons).setScale(6, java.math.RoundingMode.HALF_UP));
            } catch (NumberFormatException e) {
                vehicle.setInvoiceUaTotalPrice(null);
            }
        }
        
        if (invoiceEuPricePerTon != null && productQuantity != null) {
            try {
                BigDecimal quantityInTons = new BigDecimal(productQuantity.replace(",", "."));
                vehicle.setInvoiceEuTotalPrice(invoiceEuPricePerTon.multiply(quantityInTons).setScale(6, java.math.RoundingMode.HALF_UP));
            } catch (NumberFormatException e) {
                vehicle.setInvoiceEuTotalPrice(null);
            }
        }
        
        if (carrierId != null) {
            Carrier carrier = carrierRepository.findById(carrierId)
                    .orElseThrow(() -> new PurchaseException("CARRIER_NOT_FOUND",
                            String.format("Carrier not found: id=%d", carrierId)));
            vehicle.setCarrier(carrier);
        }
        
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

    @Transactional(readOnly = true)
    public Vehicle getVehicle(Long vehicleId) {
        return vehicleRepository.findById(vehicleId).orElse(null);
    }
    
    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByIds(List<Long> ids) {
        return vehicleRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
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
        
        if (dto.getSenderId() != null) {
            VehicleSender sender = vehicleSenderRepository.findById(dto.getSenderId())
                    .orElseThrow(() -> new PurchaseException("VEHICLE_SENDER_NOT_FOUND",
                            String.format("Vehicle sender not found: id=%d", dto.getSenderId())));
            vehicle.setSender(sender);
        } else {
            vehicle.setSender(null);
        }
        
        if (dto.getReceiverId() != null) {
            VehicleReceiver receiver = vehicleReceiverRepository.findById(dto.getReceiverId())
                    .orElseThrow(() -> new PurchaseException("VEHICLE_RECEIVER_NOT_FOUND",
                            String.format("Vehicle receiver not found: id=%d", dto.getReceiverId())));
            vehicle.setReceiver(receiver);
        } else {
            vehicle.setReceiver(null);
        }
        
        vehicle.setDestinationCountry(normalizeString(dto.getDestinationCountry()));
        vehicle.setDestinationPlace(normalizeString(dto.getDestinationPlace()));
        vehicle.setProduct(normalizeString(dto.getProduct()));
        vehicle.setProductQuantity(normalizeString(dto.getProductQuantity()));
        vehicle.setDeclarationNumber(normalizeString(dto.getDeclarationNumber()));
        vehicle.setTerminal(normalizeString(dto.getTerminal()));
        vehicle.setDriverFullName(normalizeString(dto.getDriverFullName()));
        
        if (dto.getEur1() != null) {
            vehicle.setEur1(dto.getEur1());
        }
        
        if (dto.getIsOurVehicle() != null) {
            vehicle.setIsOurVehicle(dto.getIsOurVehicle());
        }
        
        if (dto.getFito() != null) {
            vehicle.setFito(dto.getFito());
        }
        
        vehicle.setCustomsDate(dto.getCustomsDate());
        vehicle.setCustomsClearanceDate(dto.getCustomsClearanceDate());
        vehicle.setUnloadingDate(dto.getUnloadingDate());
        vehicle.setInvoiceUaDate(dto.getInvoiceUaDate());
        vehicle.setInvoiceUaPricePerTon(dto.getInvoiceUaPricePerTon());
        vehicle.setInvoiceEuDate(dto.getInvoiceEuDate());
        vehicle.setInvoiceEuPricePerTon(dto.getInvoiceEuPricePerTon());
        vehicle.setReclamation(dto.getReclamation());
        
        String productQuantity = vehicle.getProductQuantity();
        if (productQuantity != null) {
            try {
                BigDecimal quantityInTons = new BigDecimal(productQuantity.replace(",", "."));
                
                if (vehicle.getInvoiceUaPricePerTon() != null) {
                    vehicle.setInvoiceUaTotalPrice(vehicle.getInvoiceUaPricePerTon().multiply(quantityInTons).setScale(6, java.math.RoundingMode.HALF_UP));
                } else {
                    vehicle.setInvoiceUaTotalPrice(null);
                }
                
                if (vehicle.getInvoiceEuPricePerTon() != null) {
                    vehicle.setInvoiceEuTotalPrice(vehicle.getInvoiceEuPricePerTon().multiply(quantityInTons).setScale(6, java.math.RoundingMode.HALF_UP));
                } else {
                    vehicle.setInvoiceEuTotalPrice(null);
                }
            } catch (NumberFormatException e) {
                vehicle.setInvoiceUaTotalPrice(null);
                vehicle.setInvoiceEuTotalPrice(null);
            }
        } else {
            vehicle.setInvoiceUaTotalPrice(null);
            vehicle.setInvoiceEuTotalPrice(null);
        }
        
        if (dto.getCarrierId() != null) {
            Carrier carrier = carrierRepository.findById(dto.getCarrierId())
                    .orElseThrow(() -> new PurchaseException("CARRIER_NOT_FOUND",
                            String.format("Carrier not found: id=%d", dto.getCarrierId())));
            vehicle.setCarrier(carrier);
        } else if (dto.getCarrierId() == null && vehicle.getCarrier() != null) {
            vehicle.setCarrier(null);
        }
        
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

    @Transactional(readOnly = true)
    public List<VehicleProduct> getVehicleProducts(Long vehicleId) {
        return vehicleProductRepository.findByVehicleId(vehicleId);
    }

    @Transactional
    public void deleteVehicle(Long vehicleId) {
        log.info("Deleting vehicle: id={}", vehicleId);
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_NOT_FOUND",
                        String.format("Vehicle not found: id=%d", vehicleId)));

        try {
            transactionApiClient.deleteTransactionsByVehicleId(vehicleId);
            log.info("Successfully deleted all transactions for vehicle: id={}", vehicleId);
        } catch (Exception e) {
            log.error("Failed to delete transactions for vehicle: id={}, error: {}", vehicleId, e.getMessage());
            throw new PurchaseException("FAILED_TO_DELETE_TRANSACTIONS",
                    String.format("Failed to delete transactions for vehicle: %s", e.getMessage()));
        }

        List<VehicleProduct> products = vehicleProductRepository.findByVehicleId(vehicleId);

        if (!products.isEmpty()) {
            BigDecimal totalCostToSubtract = BigDecimal.ZERO;
            
            Map<String, AggregatedProduct> groupedProducts = products.stream()
                    .collect(Collectors.toMap(
                            p -> p.getWarehouseId() + "_" + p.getProductId(),
                            p -> new AggregatedProduct(p.getWarehouseId(), p.getProductId(), p.getQuantity(), p.getTotalCostEur()),
                            (existing, replacement) -> {
                                existing.quantity = existing.quantity.add(replacement.quantity);
                                existing.totalCostEur = existing.totalCostEur.add(replacement.totalCostEur);
                                return existing;
                            }
                    ));
            
            for (AggregatedProduct product : groupedProducts.values()) {
                warehouseProductBalanceService.addProduct(
                        product.warehouseId,
                        product.productId,
                        product.quantity,
                        product.totalCostEur
                );
                
                totalCostToSubtract = totalCostToSubtract.add(product.totalCostEur);
            }
            
            vehicle.subtractWithdrawalCost(totalCostToSubtract);
        }

        vehicleProductRepository.deleteAll(products);
        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted and products returned to warehouse: id={}", vehicleId);
    }

    public PageResponse<VehicleDetailsDTO> searchVehicles(String query, Pageable pageable, Map<String, List<String>> filterParams) {
        log.info("Searching vehicles: query={}, page={}, size={}, filters={}", 
                query, pageable.getPageNumber(), pageable.getPageSize(), filterParams);
        
        VehicleSpecification spec = new VehicleSpecification(query, filterParams);
        Page<Vehicle> vehiclePage = vehicleRepository.findAll(spec, pageable);
        
        List<Vehicle> vehicles = vehiclePage.getContent();
        List<Long> vehicleIds = vehicles.stream()
                .map(Vehicle::getId)
                .collect(Collectors.toList());
        
        Map<Long, List<VehicleProduct>> vehicleProductsMap = vehicleIds.isEmpty() ? 
                Collections.emptyMap() : 
                vehicleProductRepository.findByVehicleIdIn(vehicleIds).stream()
                        .collect(Collectors.groupingBy(VehicleProduct::getVehicleId));
        
        // Get expenses for all vehicles in batch
        Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesMap = vehicleIds.isEmpty() ?
                Collections.emptyMap() :
                vehicleExpenseService.getExpensesByVehicleIds(vehicleIds);
        
        // Calculate expenses total for each vehicle
        Map<Long, BigDecimal> expensesTotalMap = expensesMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .filter(e -> e.getConvertedAmount() != null)
                                .map(org.example.purchaseservice.models.balance.VehicleExpense::getConvertedAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ));
        
        List<VehicleDetailsDTO> dtos = vehicles.stream()
                .map(vehicle -> {
                    BigDecimal expensesTotal = expensesTotalMap.getOrDefault(vehicle.getId(), BigDecimal.ZERO);
                    return mapToDetailsDTO(vehicle, vehicleProductsMap.getOrDefault(vehicle.getId(), Collections.emptyList()), expensesTotal);
                })
                .collect(Collectors.toList());
        
        return new PageResponse<>(
                vehiclePage.getNumber(),
                vehiclePage.getSize(),
                vehiclePage.getTotalElements(),
                vehiclePage.getTotalPages(),
                dtos
        );
    }

    @Transactional(readOnly = true)
    public VehicleDetailsDTO mapToDetailsDTO(Vehicle vehicle) {
        List<VehicleProduct> products = vehicleProductRepository.findByVehicleId(vehicle.getId());
        // Calculate expenses total from VehicleExpense
        BigDecimal expensesTotal = BigDecimal.ZERO;
        List<org.example.purchaseservice.models.balance.VehicleExpense> expenses = vehicleExpenseService.getExpensesByVehicleId(vehicle.getId());
        if (expenses != null && !expenses.isEmpty()) {
            for (org.example.purchaseservice.models.balance.VehicleExpense expense : expenses) {
                if (expense.getConvertedAmount() != null) {
                    expensesTotal = expensesTotal.add(expense.getConvertedAmount());
                }
            }
        }
        return mapToDetailsDTO(vehicle, products, expensesTotal);
    }
    
    private VehicleDetailsDTO mapToDetailsDTO(Vehicle vehicle, List<VehicleProduct> products, BigDecimal expensesTotal) {
        
        List<VehicleDetailsDTO.VehicleItemDTO> items = products.stream()
                .map(p -> VehicleDetailsDTO.VehicleItemDTO.builder()
                        .withdrawalId(p.getId())
                        .productId(p.getProductId())
                        .productName(null)
                        .warehouseId(p.getWarehouseId())
                        .quantity(p.getQuantity())
                        .unitPriceEur(p.getUnitPriceEur())
                        .totalCostEur(p.getTotalCostEur())
                        .withdrawalDate(p.getAddedAt() != null ? p.getAddedAt().toLocalDate() : vehicle.getShipmentDate())
                        .build())
                .collect(Collectors.toList());
        
        CarrierDetailsDTO carrierDTO = null;
        if (vehicle.getCarrier() != null) {
            Carrier carrier = vehicle.getCarrier();
            carrierDTO = CarrierDetailsDTO.builder()
                    .id(carrier.getId())
                    .companyName(carrier.getCompanyName())
                    .registrationAddress(carrier.getRegistrationAddress())
                    .phoneNumber(carrier.getPhoneNumber())
                    .code(carrier.getCode())
                    .account(carrier.getAccount())
                    .createdAt(carrier.getCreatedAt())
                    .updatedAt(carrier.getUpdatedAt())
                    .build();
        }
        
        return VehicleDetailsDTO.builder()
                .id(vehicle.getId())
                .shipmentDate(vehicle.getShipmentDate())
                .vehicleNumber(vehicle.getVehicleNumber())
                .invoiceUa(vehicle.getInvoiceUa())
                .invoiceEu(vehicle.getInvoiceEu())
                .description(vehicle.getDescription())
                .totalCostEur(vehicle.getTotalCostEur())
                .userId(vehicle.getUserId())
                .createdAt(vehicle.getCreatedAt())
                .senderId(vehicle.getSender() != null ? vehicle.getSender().getId() : null)
                .senderName(vehicle.getSender() != null ? vehicle.getSender().getName() : null)
                .receiverId(vehicle.getReceiver() != null ? vehicle.getReceiver().getId() : null)
                .receiverName(vehicle.getReceiver() != null ? vehicle.getReceiver().getName() : null)
                .destinationCountry(vehicle.getDestinationCountry())
                .destinationPlace(vehicle.getDestinationPlace())
                .product(vehicle.getProduct())
                .productQuantity(vehicle.getProductQuantity())
                .declarationNumber(vehicle.getDeclarationNumber())
                .terminal(vehicle.getTerminal())
                .driverFullName(vehicle.getDriverFullName())
                .isOurVehicle(vehicle.getIsOurVehicle())
                .eur1(vehicle.getEur1())
                .fito(vehicle.getFito())
                .customsDate(vehicle.getCustomsDate())
                .customsClearanceDate(vehicle.getCustomsClearanceDate())
                .unloadingDate(vehicle.getUnloadingDate())
                .invoiceUaDate(vehicle.getInvoiceUaDate())
                .invoiceUaPricePerTon(vehicle.getInvoiceUaPricePerTon())
                .invoiceUaTotalPrice(vehicle.getInvoiceUaTotalPrice())
                .invoiceEuDate(vehicle.getInvoiceEuDate())
                .invoiceEuPricePerTon(vehicle.getInvoiceEuPricePerTon())
                .invoiceEuTotalPrice(vehicle.getInvoiceEuTotalPrice())
                .reclamation(vehicle.getReclamation())
                .totalExpenses(calculateTotalExpensesFromItems(items, expensesTotal))
                .totalIncome(calculateTotalIncome(vehicle))
                .margin(calculateMargin(vehicle, expensesTotal))
                .carrier(carrierDTO)
                .items(items)
                .build();
    }
    
    public BigDecimal calculateTotalExpenses(Vehicle vehicle, BigDecimal expensesTotal) {
        // Get products from repository
        List<VehicleProduct> products = vehicleProductRepository.findByVehicleId(vehicle.getId());
        BigDecimal totalCostEur = BigDecimal.ZERO;
        if (products != null && !products.isEmpty()) {
            for (VehicleProduct product : products) {
                if (product.getTotalCostEur() != null) {
                    totalCostEur = totalCostEur.add(product.getTotalCostEur());
                }
            }
        }
        return totalCostEur.add(expensesTotal);
    }
    
    public BigDecimal calculateTotalExpensesFromItems(List<VehicleDetailsDTO.VehicleItemDTO> items, BigDecimal expensesTotal) {
        BigDecimal totalCostEur = BigDecimal.ZERO;
        if (items != null && !items.isEmpty()) {
            for (VehicleDetailsDTO.VehicleItemDTO item : items) {
                if (item.getTotalCostEur() != null) {
                    totalCostEur = totalCostEur.add(item.getTotalCostEur());
                }
            }
        }
        return totalCostEur.add(expensesTotal);
    }
    
    public BigDecimal calculateTotalIncome(Vehicle vehicle) {
        BigDecimal invoiceEuTotalPrice = vehicle.getInvoiceEuTotalPrice() != null ? vehicle.getInvoiceEuTotalPrice() : BigDecimal.ZERO;
        BigDecimal fullReclamation = calculateFullReclamation(vehicle);
        return invoiceEuTotalPrice.subtract(fullReclamation);
    }
    
    public BigDecimal calculateFullReclamation(Vehicle vehicle) {
        BigDecimal reclamationPerTon = vehicle.getReclamation() != null ? vehicle.getReclamation() : BigDecimal.ZERO;
        if (reclamationPerTon.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        String productQuantityStr = vehicle.getProductQuantity();
        if (productQuantityStr == null || productQuantityStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            BigDecimal quantityInTons = new BigDecimal(productQuantityStr.replace(",", "."));
            return reclamationPerTon.multiply(quantityInTons).setScale(6, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse productQuantity: {}", productQuantityStr, e);
            return BigDecimal.ZERO;
        }
    }
    
    public BigDecimal calculateMargin(Vehicle vehicle, BigDecimal expensesTotal) {
        BigDecimal totalExpenses = calculateTotalExpenses(vehicle, expensesTotal);
        BigDecimal totalIncome = calculateTotalIncome(vehicle);
        return totalIncome.subtract(totalExpenses);
    }
    
    private static class AggregatedProduct {
        final Long warehouseId;
        final Long productId;
        BigDecimal quantity;
        BigDecimal totalCostEur;
        
        AggregatedProduct(Long warehouseId, Long productId, BigDecimal quantity, BigDecimal totalCostEur) {
            this.warehouseId = warehouseId;
            this.productId = productId;
            this.quantity = quantity;
            this.totalCostEur = totalCostEur;
        }
    }
}

