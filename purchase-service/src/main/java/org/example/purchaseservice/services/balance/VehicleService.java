package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.balance.Carrier;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.models.dto.balance.CarrierDetailsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleUpdateDTO;
import org.example.purchaseservice.clients.TransactionApiClient;
import org.example.purchaseservice.repositories.CarrierRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.repositories.VehicleProductRepository;
import org.example.purchaseservice.spec.VehicleSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final TransactionApiClient transactionApiClient;

    @Transactional
    public Vehicle createVehicle(LocalDate shipmentDate, String vehicleNumber,
                                    String invoiceUa, String invoiceEu,
                                    String description, Long userId, Boolean isOurVehicle,
                                    String sender, String receiver, String destinationCountry,
                                    String destinationPlace, String product, String productQuantity,
                                    String declarationNumber, String terminal, String driverFullName,
                                    Boolean eur1, Boolean fito, LocalDate customsDate,
                                    LocalDate customsClearanceDate, LocalDate unloadingDate,
                                    Long carrierId) {
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
        vehicle.setSender(normalizeString(sender));
        vehicle.setReceiver(normalizeString(receiver));
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

    public Vehicle getVehicle(Long vehicleId) {
        return vehicleRepository.findById(vehicleId).orElse(null);
    }
    
    public List<Vehicle> getVehiclesByIds(List<Long> ids) {
        return vehicleRepository.findAllById(ids);
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
        vehicle.setSender(normalizeString(dto.getSender()));
        vehicle.setReceiver(normalizeString(dto.getReceiver()));
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

    public PageResponse<VehicleDetailsDTO> searchVehicles(String query, Pageable pageable, Map<String, List<String>> filterParams) {
        log.info("Searching vehicles: query={}, page={}, size={}, filters={}", 
                query, pageable.getPageNumber(), pageable.getPageSize(), filterParams);
        
        VehicleSpecification spec = new VehicleSpecification(query, filterParams);
        Page<Vehicle> vehiclePage = vehicleRepository.findAll(spec, pageable);
        
        List<VehicleDetailsDTO> dtos = vehiclePage.getContent().stream()
                .map(this::mapToDetailsDTO)
                .collect(Collectors.toList());
        
        return new PageResponse<>(
                vehiclePage.getNumber(),
                vehiclePage.getSize(),
                vehiclePage.getTotalElements(),
                vehiclePage.getTotalPages(),
                dtos
        );
    }

    public VehicleDetailsDTO mapToDetailsDTO(Vehicle vehicle) {
        List<VehicleProduct> products = vehicleProductRepository.findByVehicleId(vehicle.getId());
        
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
                .sender(vehicle.getSender())
                .receiver(vehicle.getReceiver())
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
                .carrier(carrierDTO)
                .items(items)
                .build();
    }
}

