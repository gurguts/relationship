package org.example.purchaseservice.services.balance;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.*;
import org.example.purchaseservice.models.balance.VehicleTerminal;
import org.example.purchaseservice.models.balance.VehicleDestinationCountry;
import org.example.purchaseservice.models.balance.VehicleDestinationPlace;
import org.example.purchaseservice.models.dto.balance.VehicleUpdateDTO;
import org.example.purchaseservice.clients.TransactionApiClient;
import org.example.purchaseservice.repositories.CarrierRepository;
import org.example.purchaseservice.repositories.VehicleReceiverRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.repositories.VehicleSenderRepository;
import org.example.purchaseservice.repositories.VehicleProductRepository;
import org.example.purchaseservice.repositories.VehicleTerminalRepository;
import org.example.purchaseservice.repositories.VehicleDestinationCountryRepository;
import org.example.purchaseservice.repositories.VehicleDestinationPlaceRepository;
import org.example.purchaseservice.spec.VehicleSpecification;
import org.example.purchaseservice.utils.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import feign.FeignException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService implements IVehicleService {
    
    private static final int PRICE_SCALE = 6;
    private static final int QUANTITY_SCALE = 2;
    private static final java.math.RoundingMode PRICE_ROUNDING_MODE = java.math.RoundingMode.HALF_UP;
    private static final java.math.RoundingMode UNIT_PRICE_ROUNDING_MODE = java.math.RoundingMode.CEILING;
    
    private final VehicleRepository vehicleRepository;
    private final VehicleProductRepository vehicleProductRepository;
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    private final CarrierRepository carrierRepository;
    private final VehicleSenderRepository vehicleSenderRepository;
    private final VehicleReceiverRepository vehicleReceiverRepository;
    private final VehicleTerminalRepository vehicleTerminalRepository;
    private final VehicleDestinationCountryRepository vehicleDestinationCountryRepository;
    private final VehicleDestinationPlaceRepository vehicleDestinationPlaceRepository;
    private final TransactionApiClient transactionApiClient;
    @Getter
    private final VehicleExpenseService vehicleExpenseService;

    @Transactional
    public Vehicle createVehicle(@NonNull Vehicle vehicle) {
        log.info("Creating new vehicle: date={}, vehicle={}, invoiceUa={}, invoiceEu={}, isOurVehicle={}",
                vehicle.getShipmentDate(), vehicle.getVehicleNumber(), vehicle.getInvoiceUa(),
                vehicle.getInvoiceEu(), vehicle.getIsOurVehicle());
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle created: id={}", saved.getId());
        return saved;
    }

    @Transactional
    public Vehicle addWithdrawalCost(@NonNull Long vehicleId, BigDecimal withdrawalCost) {
        log.info("Adding withdrawal cost to vehicle: vehicleId={}, cost={}", vehicleId, withdrawalCost);
        
        Vehicle vehicle = getVehicleById(vehicleId);
        
        if (withdrawalCost != null && withdrawalCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotalCost = vehicle.getTotalCostEur();
            if (currentTotalCost == null) {
                currentTotalCost = BigDecimal.ZERO;
            }
            vehicle.setTotalCostEur(currentTotalCost.add(withdrawalCost));
        }
        
        Vehicle saved = vehicleRepository.save(vehicle);
        
        log.info("Vehicle updated: id={}, newTotalCost={}", saved.getId(), saved.getTotalCostEur());
        
        return saved;
    }

    @Transactional
    public Vehicle subtractWithdrawalCost(@NonNull Long vehicleId, BigDecimal withdrawalCost) {
        log.info("Subtracting withdrawal cost from vehicle: vehicleId={}, cost={}", vehicleId, withdrawalCost);
        
        Vehicle vehicle = getVehicleById(vehicleId);
        
        if (withdrawalCost != null && withdrawalCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotalCost = vehicle.getTotalCostEur();
            if (currentTotalCost == null) {
                currentTotalCost = BigDecimal.ZERO;
            }
            BigDecimal newTotalCost = currentTotalCost.subtract(withdrawalCost);
            if (newTotalCost.compareTo(BigDecimal.ZERO) < 0) {
                newTotalCost = BigDecimal.ZERO;
            }
            vehicle.setTotalCostEur(newTotalCost);
        }
        
        Vehicle saved = vehicleRepository.save(vehicle);
        
        log.info("Vehicle updated: id={}, newTotalCost={}", saved.getId(), saved.getTotalCostEur());
        
        return saved;
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicle(@NonNull Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_NOT_FOUND",
                        String.format("Vehicle not found: id=%d", vehicleId)));
    }
    
    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByIds(@NonNull List<Long> ids) {
        return vehicleRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByDate(@NonNull LocalDate date) {
        return vehicleRepository.findByShipmentDate(date);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getOurVehiclesByDateRange(@NonNull LocalDate fromDate, @NonNull LocalDate toDate) {
        return vehicleRepository.findOurVehiclesByDateRange(fromDate, toDate);
    }
    
    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehiclesByDateRange(@NonNull LocalDate fromDate, @NonNull LocalDate toDate) {
        return vehicleRepository.findAllVehiclesByDateRange(fromDate, toDate);
    }

    @Transactional
    public Vehicle updateVehicleProduct(@NonNull Long vehicleId, @NonNull Long vehicleProductId,
                                          BigDecimal newQuantity, BigDecimal newTotalCost) {
        validateUpdateRequest(newQuantity, newTotalCost);
        
        Vehicle vehicle = getVehicleById(vehicleId);
        VehicleProduct item = validateAndGetVehicleProduct(vehicleId, vehicleProductId);
        
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
        vehicleRepository.save(vehicle);
        
        return vehicle;
    }
    
    private void validateUpdateRequest(BigDecimal newQuantity, BigDecimal newTotalCost) {
        if ((newQuantity == null && newTotalCost == null) ||
                (newQuantity != null && newTotalCost != null)) {
            throw new PurchaseException("INVALID_UPDATE_REQUEST",
                    "Specify either quantity or total cost to update, but not both");
        }
    }
    
    private Vehicle getVehicleById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_NOT_FOUND",
                        String.format("Vehicle not found: id=%d", vehicleId)));
    }
    
    private VehicleProduct validateAndGetVehicleProduct(Long vehicleId, Long vehicleProductId) {
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
    
    private BigDecimal getOrCalculateUnitPrice(VehicleProduct item, BigDecimal oldTotalCost, BigDecimal oldQuantity) {
        BigDecimal unitPrice = item.getUnitPriceEur();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            unitPrice = calculateUnitPrice(oldTotalCost, oldQuantity);
        }
        return unitPrice;
    }
    
    private boolean updateVehicleProductQuantity(Vehicle vehicle, VehicleProduct item, BigDecimal newQuantity,
                                               BigDecimal oldQuantity, BigDecimal oldTotalCost, BigDecimal unitPrice) {
        newQuantity = newQuantity.setScale(QUANTITY_SCALE, PRICE_ROUNDING_MODE);
        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be negative");
        }
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            removeVehicleProductItem(vehicle, item, oldQuantity, oldTotalCost);
            return true;
        }
        
        BigDecimal deltaQuantity = newQuantity.subtract(oldQuantity);
        if (deltaQuantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new PurchaseException("NO_CHANGES", "Quantity has not changed");
        }
        
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
        item.setUnitPriceEur(calculateUnitPrice(newTotal, newQuantity));
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
        
        if (oldTotalCost != null && oldTotalCost.compareTo(BigDecimal.ZERO) > 0) {
            subtractVehicleTotalCost(vehicle, oldTotalCost);
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
        addVehicleTotalCost(vehicle, deltaCost);
    }
    
    private void handleQuantityDecrease(Vehicle vehicle, VehicleProduct item, 
                                        BigDecimal deltaQuantity, BigDecimal deltaCost) {
        BigDecimal returnQuantity = deltaQuantity.abs();
        BigDecimal returnCost = deltaCost.abs();
        
        warehouseProductBalanceService.addProduct(
                item.getWarehouseId(), item.getProductId(), returnQuantity, returnCost);
        subtractVehicleTotalCost(vehicle, returnCost);
    }
    
    private void updateVehicleProductTotalCost(Vehicle vehicle, VehicleProduct item, 
                                               BigDecimal newTotalCost, BigDecimal oldTotalCost, BigDecimal oldQuantity) {
        if (newTotalCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_TOTAL_COST", "Total cost must be greater than zero");
        }
        
        newTotalCost = newTotalCost.setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
        BigDecimal deltaCost = newTotalCost.subtract(oldTotalCost);
        
        if (deltaCost.compareTo(BigDecimal.ZERO) == 0) {
            throw new PurchaseException("NO_CHANGES", "Total cost has not changed");
        }
        
        warehouseProductBalanceService.adjustProductCost(
                item.getWarehouseId(), item.getProductId(), deltaCost.negate());
        
        if (deltaCost.compareTo(BigDecimal.ZERO) > 0) {
            addVehicleTotalCost(vehicle, deltaCost);
        } else {
            subtractVehicleTotalCost(vehicle, deltaCost.abs());
        }
        
        item.setTotalCostEur(newTotalCost);
        item.setUnitPriceEur(calculateUnitPrice(newTotalCost, oldQuantity));
    }

    @Transactional
    public Vehicle updateVehicle(@NonNull Long vehicleId, @NonNull VehicleUpdateDTO dto) {
        log.info("Updating vehicle: id={}", vehicleId);
        
        Vehicle vehicle = getVehicleById(vehicleId);
        
        updateBasicFields(vehicle, dto);
        updateVehicleRelations(vehicle, dto.getSenderId(), dto.getReceiverId(), dto.getCarrierId(), dto.getTerminalId(), dto.getDestinationCountryId(), dto.getDestinationPlaceId());
        updateInvoicePrices(vehicle);
        
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: id={}", saved.getId());
        return saved;
    }
    
    private void updateBasicFields(Vehicle vehicle, VehicleUpdateDTO dto) {
        updateBasicInfoFields(vehicle, dto);
        updateBooleanFields(vehicle, dto);
        updateDateAndPriceFields(vehicle, dto);
    }
    
    private void updateBasicInfoFields(Vehicle vehicle, VehicleUpdateDTO dto) {
        if (dto.getShipmentDate() != null) {
            vehicle.setShipmentDate(dto.getShipmentDate());
        }
        
        vehicle.setVehicleNumber(normalizeString(dto.getVehicleNumber()));
        vehicle.setInvoiceUa(normalizeString(dto.getInvoiceUa()));
        vehicle.setInvoiceEu(normalizeString(dto.getInvoiceEu()));
        vehicle.setDescription(normalizeString(dto.getDescription()));
        vehicle.setProduct(normalizeString(dto.getProduct()));
        vehicle.setProductQuantity(normalizeString(dto.getProductQuantity()));
        vehicle.setDeclarationNumber(normalizeString(dto.getDeclarationNumber()));
        vehicle.setDriverFullName(normalizeString(dto.getDriverFullName()));
    }
    
    private void updateBooleanFields(Vehicle vehicle, VehicleUpdateDTO dto) {
        if (dto.getEur1() != null) {
            vehicle.setEur1(dto.getEur1());
        }
        
        if (dto.getIsOurVehicle() != null) {
            vehicle.setIsOurVehicle(dto.getIsOurVehicle());
        }
        
        if (dto.getFito() != null) {
            vehicle.setFito(dto.getFito());
        }
    }
    
    private void updateDateAndPriceFields(Vehicle vehicle, VehicleUpdateDTO dto) {
        vehicle.setCustomsDate(dto.getCustomsDate());
        vehicle.setCustomsClearanceDate(dto.getCustomsClearanceDate());
        vehicle.setUnloadingDate(dto.getUnloadingDate());
        vehicle.setInvoiceUaDate(dto.getInvoiceUaDate());
        vehicle.setInvoiceUaPricePerTon(dto.getInvoiceUaPricePerTon());
        vehicle.setInvoiceEuDate(dto.getInvoiceEuDate());
        vehicle.setInvoiceEuPricePerTon(dto.getInvoiceEuPricePerTon());
        vehicle.setReclamation(dto.getReclamation());
    }
    
    private String normalizeString(String value) {
        return StringUtils.normalizeString(value);
    }

    @Transactional
    public Vehicle addProductToVehicle(@NonNull Long vehicleId, @NonNull Long warehouseId, 
                                                 @NonNull Long productId, @NonNull BigDecimal quantity, Long userId) {
        log.info("Adding product to vehicle: vehicleId={}, warehouseId={}, productId={}, quantity={}", 
                vehicleId, warehouseId, productId, quantity);

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity must be greater than zero");
        }

        Vehicle vehicle = getVehicleById(vehicleId);
        WarehouseProductBalance balance =
                validateAndGetBalance(warehouseId, productId, quantity);
        
        quantity = quantity.setScale(QUANTITY_SCALE, PRICE_ROUNDING_MODE);
        BigDecimal totalCost = calculateTotalCost(quantity, balance.getAveragePriceEur());
        
        logBalanceBeforeRemoval(warehouseId, productId, quantity, totalCost, balance);
        warehouseProductBalanceService.removeProductWithCost(warehouseId, productId, quantity, totalCost);
        
        VehicleProduct vehicleProduct = createVehicleProduct(vehicleId, warehouseId, productId, 
                quantity, balance.getAveragePriceEur(), totalCost, userId);
        vehicleProductRepository.save(vehicleProduct);
        
        addVehicleTotalCost(vehicle, totalCost);
        Vehicle saved = vehicleRepository.save(vehicle);
        
        log.info("Product added to vehicle: vehicleId={}, totalCost={}", saved.getId(), totalCost);
        
        return saved;
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
    
    private BigDecimal calculateTotalCost(BigDecimal quantity, BigDecimal averagePrice) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity must be positive");
        }
        if (averagePrice == null) {
            throw new PurchaseException("INVALID_AVERAGE_PRICE", "Average price cannot be null");
        }
        return quantity.multiply(averagePrice).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
    }
    
    private void logBalanceBeforeRemoval(Long warehouseId, Long productId, BigDecimal quantity, 
                                         BigDecimal totalCost, WarehouseProductBalance balance) {
        log.info("Before removeProductWithCost: warehouseId={}, productId={}, quantity={}, averagePrice={}, totalCost={}, currentBalanceQuantity={}, currentBalanceTotalCost={}",
                warehouseId, productId, quantity, balance.getAveragePriceEur(), totalCost, balance.getQuantity(), balance.getTotalCostEur());
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

    @Transactional(readOnly = true)
    public List<VehicleProduct> getVehicleProducts(@NonNull Long vehicleId) {
        return vehicleProductRepository.findByVehicleId(vehicleId);
    }

    @Transactional
    public void deleteVehicle(@NonNull Long vehicleId) {
        log.info("Deleting vehicle: id={}", vehicleId);
        
        Vehicle vehicle = getVehicleById(vehicleId);

        try {
            transactionApiClient.deleteTransactionsByVehicleId(vehicleId);
            log.info("Successfully deleted all transactions for vehicle: id={}", vehicleId);
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

        List<VehicleProduct> products = vehicleProductRepository.findByVehicleId(vehicleId);

        if (!products.isEmpty()) {
            returnProductsToWarehouse(vehicle, products);
        }

        vehicleProductRepository.deleteAll(products);
        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted and products returned to warehouse: id={}", vehicleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Vehicle> searchVehicles(String query, @NonNull Pageable pageable, Map<String, List<String>> filterParams) {
        log.info("Searching vehicles: query={}, page={}, size={}, filters={}", 
                query, pageable.getPageNumber(), pageable.getPageSize(), filterParams);
        
        VehicleSpecification spec = new VehicleSpecification(query, filterParams);
        return vehicleRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalExpenses(@NonNull Vehicle vehicle, @NonNull BigDecimal expensesTotal) {
        List<VehicleProduct> products = vehicleProductRepository.findByVehicleId(vehicle.getId());
        return calculateTotalExpenses(products, expensesTotal);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalExpenses(@NonNull List<VehicleProduct> products, @NonNull BigDecimal expensesTotal) {
        BigDecimal totalCostEur = products.stream()
                .map(VehicleProduct::getTotalCostEur)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalCostEur.add(expensesTotal);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalIncome(@NonNull Vehicle vehicle) {
        BigDecimal invoiceEuTotalPrice = vehicle.getInvoiceEuTotalPrice() != null ? vehicle.getInvoiceEuTotalPrice() : BigDecimal.ZERO;
        BigDecimal fullReclamation = calculateFullReclamation(vehicle);
        return invoiceEuTotalPrice.subtract(fullReclamation);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal calculateFullReclamation(@NonNull Vehicle vehicle) {
        BigDecimal reclamationPerTon = vehicle.getReclamation() != null ? vehicle.getReclamation() : BigDecimal.ZERO;
        if (reclamationPerTon.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        String productQuantityStr = vehicle.getProductQuantity();
        BigDecimal quantityInTons = parseProductQuantity(productQuantityStr);
        if (quantityInTons == null) {
            return BigDecimal.ZERO;
        }
        return reclamationPerTon.multiply(quantityInTons).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal calculateMargin(@NonNull Vehicle vehicle, @NonNull BigDecimal expensesTotal) {
        BigDecimal totalExpenses = calculateTotalExpenses(vehicle, expensesTotal);
        BigDecimal totalIncome = calculateTotalIncome(vehicle);
        return totalIncome.subtract(totalExpenses);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal calculateMargin(@NonNull Vehicle vehicle, @NonNull List<VehicleProduct> products, @NonNull BigDecimal expensesTotal) {
        BigDecimal totalExpenses = calculateTotalExpenses(products, expensesTotal);
        BigDecimal totalIncome = calculateTotalIncome(vehicle);
        return totalIncome.subtract(totalExpenses);
    }

    private record AggregatedProduct(Long warehouseId, Long productId, BigDecimal quantity, BigDecimal totalCostEur) {}
    
    private record WarehouseProductKey(Long warehouseId, Long productId) {}
    
    private BigDecimal calculateUnitPrice(BigDecimal totalCost, BigDecimal quantity) {
        if (totalCost == null) {
            throw new PurchaseException("INVALID_TOTAL_COST", "Total cost cannot be null");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Cannot calculate unit price: quantity is zero or null");
        }
        return totalCost.divide(quantity, PRICE_SCALE, UNIT_PRICE_ROUNDING_MODE);
    }
    
    private void addVehicleTotalCost(Vehicle vehicle, BigDecimal cost) {
        if (cost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotalCost = vehicle.getTotalCostEur();
            if (currentTotalCost == null) {
                currentTotalCost = BigDecimal.ZERO;
            }
            vehicle.setTotalCostEur(currentTotalCost.add(cost));
        }
    }
    
    private void subtractVehicleTotalCost(Vehicle vehicle, BigDecimal cost) {
        if (cost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotalCost = vehicle.getTotalCostEur();
            if (currentTotalCost == null) {
                currentTotalCost = BigDecimal.ZERO;
            }
            BigDecimal newTotalCost = currentTotalCost.subtract(cost);
            if (newTotalCost.compareTo(BigDecimal.ZERO) < 0) {
                newTotalCost = BigDecimal.ZERO;
            }
            vehicle.setTotalCostEur(newTotalCost);
        }
    }
    
    private void updateVehicleRelations(Vehicle vehicle, Long senderId, Long receiverId, Long carrierId, Long terminalId, Long destinationCountryId, Long destinationPlaceId) {
        updateVehicleSender(vehicle, senderId);
        updateVehicleReceiver(vehicle, receiverId);
        updateVehicleCarrier(vehicle, carrierId);
        updateVehicleTerminal(vehicle, terminalId);
        updateVehicleDestinationCountry(vehicle, destinationCountryId);
        updateVehicleDestinationPlace(vehicle, destinationPlaceId);
    }
    
    private void updateVehicleSender(Vehicle vehicle, Long senderId) {
        if (senderId != null) {
            VehicleSender sender = vehicleSenderRepository.findById(senderId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_SENDER_NOT_FOUND",
                            String.format("Vehicle sender not found: id=%d", senderId)));
            vehicle.setSender(sender);
        } else {
            vehicle.setSender(null);
        }
    }
    
    private void updateVehicleReceiver(Vehicle vehicle, Long receiverId) {
        if (receiverId != null) {
            VehicleReceiver receiver = vehicleReceiverRepository.findById(receiverId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_RECEIVER_NOT_FOUND",
                            String.format("Vehicle receiver not found: id=%d", receiverId)));
            vehicle.setReceiver(receiver);
        } else {
            vehicle.setReceiver(null);
        }
    }
    
    private void updateVehicleCarrier(Vehicle vehicle, Long carrierId) {
        if (carrierId != null) {
            Carrier carrier = carrierRepository.findById(carrierId)
                    .orElseThrow(() -> new PurchaseException("CARRIER_NOT_FOUND",
                            String.format("Carrier not found: id=%d", carrierId)));
            vehicle.setCarrier(carrier);
        } else if (vehicle.getCarrier() != null) {
            vehicle.setCarrier(null);
        }
    }

    private void updateVehicleTerminal(Vehicle vehicle, Long terminalId) {
        if (terminalId != null) {
            VehicleTerminal terminal = vehicleTerminalRepository.findById(terminalId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_TERMINAL_NOT_FOUND",
                            String.format("Vehicle terminal not found: id=%d", terminalId)));
            vehicle.setTerminal(terminal);
        } else {
            vehicle.setTerminal(null);
        }
    }

    private void updateVehicleDestinationCountry(Vehicle vehicle, Long destinationCountryId) {
        if (destinationCountryId != null) {
            VehicleDestinationCountry country = vehicleDestinationCountryRepository.findById(destinationCountryId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_DESTINATION_COUNTRY_NOT_FOUND",
                            String.format("Vehicle destination country not found: id=%d", destinationCountryId)));
            vehicle.setDestinationCountry(country);
        } else {
            vehicle.setDestinationCountry(null);
        }
    }

    private void updateVehicleDestinationPlace(Vehicle vehicle, Long destinationPlaceId) {
        if (destinationPlaceId != null) {
            VehicleDestinationPlace place = vehicleDestinationPlaceRepository.findById(destinationPlaceId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_DESTINATION_PLACE_NOT_FOUND",
                            String.format("Vehicle destination place not found: id=%d", destinationPlaceId)));
            vehicle.setDestinationPlace(place);
        } else {
            vehicle.setDestinationPlace(null);
        }
    }
    
    private void returnProductsToWarehouse(Vehicle vehicle, List<VehicleProduct> products) {
        if (products.isEmpty()) {
            return;
        }
        
        Map<WarehouseProductKey, AggregatedProduct> groupedProducts = groupProductsByWarehouseAndProduct(products);
        BigDecimal totalCostToSubtract = returnGroupedProductsToWarehouse(groupedProducts);
        
        if (totalCostToSubtract.compareTo(BigDecimal.ZERO) > 0) {
            subtractVehicleTotalCost(vehicle, totalCostToSubtract);
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
    
    private void updateInvoicePrices(Vehicle vehicle) {
        String productQuantity = vehicle.getProductQuantity();
        BigDecimal quantityInTons = parseProductQuantity(productQuantity);
        
        if (quantityInTons != null) {
            if (vehicle.getInvoiceUaPricePerTon() != null) {
                vehicle.setInvoiceUaTotalPrice(vehicle.getInvoiceUaPricePerTon()
                        .multiply(quantityInTons).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE));
            } else {
                vehicle.setInvoiceUaTotalPrice(null);
            }
            
            if (vehicle.getInvoiceEuPricePerTon() != null) {
                vehicle.setInvoiceEuTotalPrice(vehicle.getInvoiceEuPricePerTon()
                        .multiply(quantityInTons).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE));
            } else {
                vehicle.setInvoiceEuTotalPrice(null);
            }
        } else {
            vehicle.setInvoiceUaTotalPrice(null);
            vehicle.setInvoiceEuTotalPrice(null);
        }
    }
    
    private BigDecimal parseProductQuantity(String productQuantity) {
        if (productQuantity == null || productQuantity.trim().isEmpty()) {
            return null;
        }
        
        try {
            return new BigDecimal(productQuantity.replace(",", "."));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse productQuantity: {}", productQuantity, e);
            return null;
        }
    }
}

