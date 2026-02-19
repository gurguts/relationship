package org.example.purchaseservice.services.vehicle;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.models.dto.balance.OurVehiclesStatsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleUpdateDTO;
import org.example.purchaseservice.repositories.VehicleProductRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.services.impl.IVehicleExpenseService;
import org.example.purchaseservice.services.impl.IVehicleService;
import org.example.purchaseservice.spec.StockVehicleSpecification;
import org.example.purchaseservice.spec.VehicleFilterBuilder;
import org.example.purchaseservice.spec.VehicleSearchPredicateBuilder;
import org.example.purchaseservice.spec.VehicleSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService implements IVehicleService {
    
    private final VehicleRepository vehicleRepository;
    private final VehicleProductRepository vehicleProductRepository;
    private final VehicleValidator validator;
    private final VehicleCostCalculator costCalculator;
    private final VehicleProductService productService;
    private final VehicleUpdateService updateService;
    private final VehicleDeletionService deletionService;
    @Getter
    private final IVehicleExpenseService vehicleExpenseService;
    private final VehicleFilterBuilder filterBuilder;
    private final VehicleSearchPredicateBuilder searchPredicateBuilder;

    @Transactional
    public Vehicle createVehicle(@NonNull Vehicle vehicle) {
        log.info("Creating new vehicle: date={}, vehicle={}, invoiceUa={}, invoiceEu={}, managerId={}",
                vehicle.getShipmentDate(), vehicle.getVehicleNumber(), vehicle.getInvoiceUa(),
                vehicle.getInvoiceEu(), vehicle.getManagerId());
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle created: id={}", saved.getId());
        return saved;
    }

    @Transactional
    public void addWithdrawalCost(@NonNull Long vehicleId, BigDecimal withdrawalCost) {
        log.info("Adding withdrawal cost to vehicle: vehicleId={}, cost={}", vehicleId, withdrawalCost);
        
        Vehicle vehicle = validator.validateAndGetVehicle(vehicleId);
        
        if (withdrawalCost != null && withdrawalCost.compareTo(BigDecimal.ZERO) > 0) {
            costCalculator.addVehicleTotalCost(vehicle, withdrawalCost);
        }
        
        Vehicle saved = vehicleRepository.save(vehicle);
        logVehicleCostUpdated(saved);
    }

    @Transactional
    public void subtractWithdrawalCost(@NonNull Long vehicleId, BigDecimal withdrawalCost) {
        log.info("Subtracting withdrawal cost from vehicle: vehicleId={}, cost={}", vehicleId, withdrawalCost);
        
        Vehicle vehicle = validator.validateAndGetVehicle(vehicleId);
        
        if (withdrawalCost != null && withdrawalCost.compareTo(BigDecimal.ZERO) > 0) {
            costCalculator.subtractVehicleTotalCost(vehicle, withdrawalCost);
        }
        
        Vehicle saved = vehicleRepository.save(vehicle);
        logVehicleCostUpdated(saved);
    }
    
    private void logVehicleCostUpdated(@NonNull Vehicle vehicle) {
        log.info("Vehicle updated: id={}, newTotalCost={}", vehicle.getId(), vehicle.getTotalCostEur());
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicle(@NonNull Long vehicleId) {
        return validator.validateAndGetVehicle(vehicleId);
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

    @Override
    @Transactional(readOnly = true)
    public Page<Vehicle> findOurVehiclesPaged(String query, LocalDate fromDate, LocalDate toDate, List<Long> managerIds, @NonNull Pageable pageable) {
        StockVehicleSpecification spec = new StockVehicleSpecification(query, fromDate, toDate, managerIds);
        return vehicleRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public OurVehiclesStatsDTO getOurVehiclesStats(String query, LocalDate fromDate, LocalDate toDate, List<Long> managerIds) {
        List<Long> managerIdsList = managerIds != null ? managerIds : Collections.emptyList();
        StockVehicleSpecification spec = new StockVehicleSpecification(query, fromDate, toDate, managerIdsList);
        Sort sort = Sort.by(Sort.Direction.DESC, "shipmentDate");
        List<Vehicle> vehicles = vehicleRepository.findAll(spec, sort);
        if (vehicles.isEmpty()) {
            return new OurVehiclesStatsDTO(0L, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        List<Long> vehicleIds = vehicles.stream().map(Vehicle::getId).toList();
        Map<Long, List<VehicleProduct>> productsByVehicle = getVehicleProductsByVehicleIds(vehicleIds);
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (List<VehicleProduct> products : productsByVehicle.values()) {
            for (VehicleProduct vp : products) {
                if (vp.getQuantity() != null) totalQuantity = totalQuantity.add(vp.getQuantity());
                if (vp.getTotalCostEur() != null) totalCost = totalCost.add(vp.getTotalCostEur());
            }
        }
        return new OurVehiclesStatsDTO(vehicles.size(), totalQuantity, totalCost);
    }
    
    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehiclesByDateRange(@NonNull LocalDate fromDate, @NonNull LocalDate toDate) {
        return vehicleRepository.findAllVehiclesByDateRange(fromDate, toDate);
    }

    @Transactional
    public Vehicle updateVehicleProduct(@NonNull Long vehicleId, @NonNull Long vehicleProductId,
                                        BigDecimal newQuantity, BigDecimal newTotalCost) {
        return productService.updateVehicleProduct(vehicleId, vehicleProductId, newQuantity, newTotalCost);
    }

    @Transactional
    public Vehicle updateVehicle(@NonNull Long vehicleId, @NonNull VehicleUpdateDTO dto) {
        log.info("Updating vehicle: id={}", vehicleId);
        
        Vehicle vehicle = validator.validateAndGetVehicle(vehicleId);
        updateService.updateVehicle(vehicle, dto);
        
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: id={}", saved.getId());
        return saved;
    }
    
    @Transactional
    public Vehicle addProductToVehicle(@NonNull Long vehicleId, @NonNull Long warehouseId, 
                                     @NonNull Long productId, @NonNull BigDecimal quantity, Long userId) {
        return productService.addProductToVehicle(vehicleId, warehouseId, productId, quantity, userId);
    }

    @Transactional(readOnly = true)
    public List<VehicleProduct> getVehicleProducts(@NonNull Long vehicleId) {
        return vehicleProductRepository.findByVehicleId(vehicleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<VehicleProduct>> getVehicleProductsByVehicleIds(@NonNull List<Long> vehicleIds) {
        if (vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<VehicleProduct> products = vehicleProductRepository.findByVehicleIdIn(vehicleIds);
        return products.stream()
                .collect(java.util.stream.Collectors.groupingBy(VehicleProduct::getVehicleId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> getExpensesTotalsByVehicleIds(@NonNull List<Long> vehicleIds) {
        return costCalculator.getExpensesTotalsByVehicleIds(vehicleIds);
    }

    @Transactional
    public void deleteVehicle(@NonNull Long vehicleId) {
        deletionService.deleteVehicle(vehicleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Vehicle> searchVehicles(String query, @NonNull Pageable pageable, Map<String, List<String>> filterParams) {
        String sortField = null;
        org.springframework.data.domain.Sort.Direction sortDirection = null;
        if (pageable.getSort().isSorted()) {
            org.springframework.data.domain.Sort.Order order = pageable.getSort().iterator().next();
            sortField = order.getProperty();
            sortDirection = order.getDirection();
        }

        VehicleSpecification spec = new VehicleSpecification(
                query,
                filterParams,
                filterBuilder,
                searchPredicateBuilder,
                sortField,
                sortDirection
        );

        Pageable pageableWithoutSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return vehicleRepository.findAll(spec, pageableWithoutSort);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalExpenses(@NonNull List<VehicleProduct> products, @NonNull BigDecimal expensesTotal) {
        return costCalculator.calculateTotalExpenses(products, expensesTotal);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalIncome(@NonNull Vehicle vehicle) {
        return costCalculator.calculateTotalIncome(vehicle);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateMargin(@NonNull Vehicle vehicle, @NonNull List<VehicleProduct> products, 
                                     @NonNull BigDecimal expensesTotal) {
        return costCalculator.calculateMargin(vehicle, products, expensesTotal);
    }
}
