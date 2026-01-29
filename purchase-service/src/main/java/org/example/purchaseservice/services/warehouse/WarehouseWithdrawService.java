package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.mappers.WarehouseWithdrawMapper;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalDTO;
import org.example.purchaseservice.repositories.WarehouseWithdrawalRepository;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.example.purchaseservice.services.impl.IVehicleService;
import org.example.purchaseservice.services.impl.IWarehouseProductBalanceService;
import org.example.purchaseservice.services.impl.IWarehouseWithdrawService;
import org.example.purchaseservice.spec.WarehouseWithdrawalFilterBuilder;
import org.example.purchaseservice.spec.WarehouseWithdrawalSpecification;
import org.example.purchaseservice.utils.SecurityUtils;
import org.example.purchaseservice.utils.ValidationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseWithdrawService implements IWarehouseWithdrawService {
    
    private static final int MAX_PAGE_SIZE = 1000;
    
    private final WarehouseWithdrawalRepository warehouseWithdrawalRepository;
    private final WithdrawalReasonRepository withdrawalReasonRepository;
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    private final IVehicleService vehicleService;
    private final WarehouseWithdrawValidator validator;
    private final WarehouseWithdrawCalculator calculator;
    private final WarehouseWithdrawBalanceHandler balanceHandler;
    private final WarehouseWithdrawFactory factory;
    private final WarehouseWithdrawMapper mapper;
    private final WarehouseWithdrawalFilterBuilder filterBuilder;

    @Override
    @Transactional
    public WarehouseWithdrawal createWithdrawal(@NonNull WarehouseWithdrawal warehouseWithdrawal) {
        log.info("Creating warehouse withdrawal: warehouseId={}, productId={}, quantity={}", 
                warehouseWithdrawal.getWarehouseId(), warehouseWithdrawal.getProductId(), warehouseWithdrawal.getQuantity());
        validator.validateWarehouseWithdrawal(warehouseWithdrawal);
        
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new PurchaseException("USER_NOT_FOUND", "Current user ID is null");
        }
        
        warehouseWithdrawal.setUserId(userId);
        
        BigDecimal quantity = calculator.validateAndScaleQuantity(warehouseWithdrawal.getQuantity());
        warehouseWithdrawal.setQuantity(quantity);
        
        WarehouseProductBalance balance = warehouseProductBalanceService.getBalance(
                warehouseWithdrawal.getWarehouseId(),
                warehouseWithdrawal.getProductId());
        
        balance = validator.validateAndGetBalance(balance
        );
        
        BigDecimal availableQuantity = balance.getQuantity();
        validator.validateAvailableQuantity(availableQuantity, quantity, warehouseWithdrawal.getVehicleId());
        
        WarehouseWithdrawBalanceHandler.PriceCalculationResult priceResult = balanceHandler.calculatePriceAndRemoveProduct(
                warehouseWithdrawal, balance, quantity);
        
        warehouseWithdrawal.setUnitPriceEur(priceResult.unitPrice());
        warehouseWithdrawal.setTotalCostEur(priceResult.totalCost());
        
        factory.setWithdrawalDateIfNeeded(warehouseWithdrawal);
        
        WarehouseWithdrawal savedWithdrawal = warehouseWithdrawalRepository.save(warehouseWithdrawal);
        
        if (warehouseWithdrawal.getVehicleId() != null) {
            vehicleService.addWithdrawalCost(warehouseWithdrawal.getVehicleId(), priceResult.totalCost());
        }
        
        log.info("Warehouse withdrawal created: id={}", savedWithdrawal.getId());
        return savedWithdrawal;
    }

    @Override
    @Transactional
    public WarehouseWithdrawal updateWithdrawal(@NonNull Long id, @NonNull WarehouseWithdrawal request) {
        log.info("Updating warehouse withdrawal: id={}", id);
        validator.validateWarehouseWithdrawalForUpdate(request);
        
        WarehouseWithdrawal withdrawal = findWithdrawalById(id);
        
        validator.validateProductChange(request, withdrawal);
        
        BigDecimal originalQuantity = calculator.validateAndScaleQuantity(withdrawal.getQuantity());
        BigDecimal unitPrice = calculator.resolveUnitPrice(withdrawal);
        
        BigDecimal newQuantity = request.getQuantity() != null
                ? calculator.validateAndScaleQuantity(request.getQuantity())
                : originalQuantity;
        
        validator.validateQuantity(newQuantity);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            balanceHandler.restoreWithdrawalToWarehouse(withdrawal);
            warehouseWithdrawalRepository.delete(withdrawal);
            log.info("Warehouse withdrawal deleted (quantity=0): id={}", id);
            return null;
        }
        
        BigDecimal delta = newQuantity.subtract(originalQuantity);
        
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            WarehouseProductBalance balance = warehouseProductBalanceService.getBalance(
                    withdrawal.getWarehouseId(), withdrawal.getProductId());
            balance = validator.validateAndGetBalance(balance
            );
            balanceHandler.handleQuantityIncrease(withdrawal, delta, unitPrice, balance);
        } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
            balanceHandler.handleQuantityDecrease(withdrawal, delta.abs(), unitPrice);
        }
        
        factory.updateWithdrawalFields(withdrawal, request, newQuantity, unitPrice);
        
        WarehouseWithdrawal saved = warehouseWithdrawalRepository.save(withdrawal);
        log.info("Warehouse withdrawal updated: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void deleteWithdrawal(@NonNull Long id) {
        log.info("Deleting warehouse withdrawal: id={}", id);
        WarehouseWithdrawal withdrawal = findWithdrawalById(id);
        balanceHandler.restoreWithdrawalToWarehouse(withdrawal);
        warehouseWithdrawalRepository.delete(withdrawal);
        log.info("Warehouse withdrawal deleted: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WithdrawalDTO> getWithdrawals(
            int page, 
            int size, 
            String sort,
            String direction,
            Map<String, List<String>> filters) {
        
        ValidationUtils.validatePage(page);
        ValidationUtils.validatePageSize(size, MAX_PAGE_SIZE);
        ValidationUtils.validateSortParams(sort, direction);
        ValidationUtils.validateFilters(filters);
        
        Sort sortOrder = Sort.by(Sort.Direction.fromString(direction), sort);
        Pageable pageRequest = PageRequest.of(page, size, sortOrder);
        WarehouseWithdrawalSpecification spec = new WarehouseWithdrawalSpecification(filters, filterBuilder);
        Page<WarehouseWithdrawal> withdrawalPage = warehouseWithdrawalRepository.findAll(spec, pageRequest);

        List<WithdrawalDTO> content = withdrawalPage.getContent().stream()
                .map(mapper::convertToDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                withdrawalPage.getNumber(),
                withdrawalPage.getSize(),
                withdrawalPage.getTotalElements(),
                withdrawalPage.getTotalPages(),
                content
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawalReason> getAllWithdrawalReasons() {
        return withdrawalReasonRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawalReason> getWithdrawalReasonsByPurpose(@NonNull WithdrawalReason.Purpose purpose) {
        return withdrawalReasonRepository.findByPurpose(purpose);
    }

    private WarehouseWithdrawal findWithdrawalById(@NonNull Long id) {
        return warehouseWithdrawalRepository.findById(id)
                .orElseThrow(() -> new PurchaseException("WITHDRAWAL_NOT_FOUND", 
                        String.format("Withdrawal with ID %d not found", id)));
    }
}
