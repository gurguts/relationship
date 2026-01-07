package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalDTO;
import org.example.purchaseservice.repositories.WarehouseWithdrawalRepository;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.example.purchaseservice.services.impl.IWarehouseWithdrawService;
import org.example.purchaseservice.spec.WarehouseWithdrawalSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.example.purchaseservice.utils.SecurityUtils;
import org.example.purchaseservice.utils.ValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseWithdrawService implements IWarehouseWithdrawService {
    
    private static final int QUANTITY_SCALE = 2;
    private static final int PRICE_SCALE = 6;
    private static final int MAX_PAGE_SIZE = 1000;
    private static final BigDecimal MINIMUM_REMAINING = BigDecimal.ONE;
    private static final RoundingMode QUANTITY_ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final RoundingMode PRICE_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    private final WarehouseWithdrawalRepository warehouseWithdrawalRepository;
    private final WithdrawalReasonRepository withdrawalReasonRepository;
    private final org.example.purchaseservice.services.balance.IWarehouseProductBalanceService warehouseProductBalanceService;
    private final org.example.purchaseservice.services.balance.IVehicleService vehicleService;

    @Override
    @Transactional
    public WarehouseWithdrawal createWithdrawal(@NonNull WarehouseWithdrawal warehouseWithdrawal) {
        validateWarehouseWithdrawal(warehouseWithdrawal);
        
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new PurchaseException("USER_NOT_FOUND", "Current user ID is null");
        }
        
        warehouseWithdrawal.setUserId(userId);
        
        BigDecimal quantity = validateAndScaleQuantity(warehouseWithdrawal.getQuantity());
        warehouseWithdrawal.setQuantity(quantity);
        
        WarehouseProductBalance balance = validateAndGetBalance(
                warehouseWithdrawal.getWarehouseId(),
                warehouseWithdrawal.getProductId());
        
        BigDecimal availableQuantity = balance.getQuantity();
        validateAvailableQuantity(availableQuantity, quantity, warehouseWithdrawal.getVehicleId());
        
        PriceCalculationResult priceResult = calculatePriceAndRemoveProduct(
                warehouseWithdrawal, balance, quantity);
        
        warehouseWithdrawal.setUnitPriceEur(priceResult.unitPrice());
        warehouseWithdrawal.setTotalCostEur(priceResult.totalCost());
        
        setWithdrawalDateIfNeeded(warehouseWithdrawal);
        
        WarehouseWithdrawal savedWithdrawal = warehouseWithdrawalRepository.save(warehouseWithdrawal);
        
        if (warehouseWithdrawal.getVehicleId() != null) {
            vehicleService.addWithdrawalCost(warehouseWithdrawal.getVehicleId(), priceResult.totalCost());
        }
        
        return savedWithdrawal;
    }

    @Override
    @Transactional
    public WarehouseWithdrawal updateWithdrawal(@NonNull Long id, @NonNull WarehouseWithdrawal request) {
        validateWarehouseWithdrawalForUpdate(request);
        
        WarehouseWithdrawal withdrawal = findWithdrawalById(id);
        
        validateProductChange(request, withdrawal);
        
        BigDecimal originalQuantity = validateAndScaleQuantity(withdrawal.getQuantity());
        BigDecimal unitPrice = resolveUnitPrice(withdrawal);
        
        BigDecimal newQuantity = request.getQuantity() != null
                ? validateAndScaleQuantity(request.getQuantity())
                : originalQuantity;
        
        validateQuantity(newQuantity);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            restoreWithdrawalToWarehouse(withdrawal);
            warehouseWithdrawalRepository.delete(withdrawal);
            return null;
        }
        
        BigDecimal delta = newQuantity.subtract(originalQuantity);
        
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            handleQuantityIncrease(withdrawal, delta, unitPrice);
        } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
            handleQuantityDecrease(withdrawal, delta.abs(), unitPrice);
        }
        
        updateWithdrawalFields(withdrawal, request, newQuantity, unitPrice);
        
        return warehouseWithdrawalRepository.save(withdrawal);
    }

    @Override
    @Transactional
    public void deleteWithdrawal(@NonNull Long id) {
        WarehouseWithdrawal withdrawal = findWithdrawalById(id);
        restoreWithdrawalToWarehouse(withdrawal);
        warehouseWithdrawalRepository.delete(withdrawal);
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
        WarehouseWithdrawalSpecification spec = new WarehouseWithdrawalSpecification(filters);
        Page<WarehouseWithdrawal> withdrawalPage = warehouseWithdrawalRepository.findAll(spec, pageRequest);

        List<WithdrawalDTO> content = withdrawalPage.getContent().stream()
                .map(this::convertToDTO)
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
        try {
            return withdrawalReasonRepository.findAll();
        } catch (Exception e) {
            log.error("Error getting all withdrawal reasons", e);
            throw new PurchaseException("FAILED_TO_GET_WITHDRAWAL_REASONS", 
                    "Failed to get all withdrawal reasons", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawalReason> getWithdrawalReasonsByPurpose(@NonNull WithdrawalReason.Purpose purpose) {
        try {
            return withdrawalReasonRepository.findByPurpose(purpose);
        } catch (Exception e) {
            log.error("Error getting withdrawal reasons by purpose: purpose={}", purpose, e);
            throw new PurchaseException("FAILED_TO_GET_WITHDRAWAL_REASONS", 
                    String.format("Failed to get withdrawal reasons by purpose: %s", purpose), e);
        }
    }

    private void validateWarehouseWithdrawal(@NonNull WarehouseWithdrawal warehouseWithdrawal) {
        if (warehouseWithdrawal.getWarehouseId() == null) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Warehouse ID cannot be null");
        }
        if (warehouseWithdrawal.getProductId() == null) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Product ID cannot be null");
        }
        if (warehouseWithdrawal.getQuantity() == null) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Quantity cannot be null");
        }
        if (warehouseWithdrawal.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Quantity must be positive");
        }
    }

    private void validateWarehouseWithdrawalForUpdate(@NonNull WarehouseWithdrawal warehouseWithdrawal) {
        if (warehouseWithdrawal.getWarehouseId() == null) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Warehouse ID cannot be null");
        }
        if (warehouseWithdrawal.getProductId() == null) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Product ID cannot be null");
        }
        if (warehouseWithdrawal.getQuantity() != null && warehouseWithdrawal.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Quantity cannot be negative");
        }
    }

    private BigDecimal validateAndScaleQuantity(@NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity must be positive");
        }
        return quantity.setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE);
    }

    private void validateQuantity(@NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be negative");
        }
    }

    private WarehouseProductBalance validateAndGetBalance(@NonNull Long warehouseId, @NonNull Long productId) {
        WarehouseProductBalance balance = warehouseProductBalanceService.getBalance(warehouseId, productId);
        
        if (balance == null) {
            throw new PurchaseException("INSUFFICIENT_PRODUCT", 
                    String.format("Insufficient product on warehouse. Available: 0, warehouseId: %d, productId: %d", 
                            warehouseId, productId));
        }
        
        if (balance.getQuantity() == null) {
            throw new PurchaseException("INVALID_BALANCE", "Balance quantity cannot be null");
        }
        
        if (balance.getAveragePriceEur() == null) {
            throw new PurchaseException("INVALID_BALANCE", "Balance average price cannot be null");
        }
        
        return balance;
    }

    private void validateAvailableQuantity(
            @NonNull BigDecimal availableQuantity,
            @NonNull BigDecimal requestedQuantity,
            Long vehicleId) {
        
        if (availableQuantity.compareTo(requestedQuantity) < 0) {
            throw new PurchaseException("INSUFFICIENT_PRODUCT", 
                    String.format("Insufficient product on warehouse. Available: %s, requested: %s",
                            availableQuantity, requestedQuantity));
        }
        
        if (vehicleId == null) {
            validateMinimumRemaining(availableQuantity, requestedQuantity);
        }
    }

    private void validateMinimumRemaining(
            @NonNull BigDecimal availableQuantity,
            @NonNull BigDecimal requestedQuantity) {
        
        BigDecimal remainingAfterWithdrawal = availableQuantity.subtract(requestedQuantity);
        if (remainingAfterWithdrawal.compareTo(MINIMUM_REMAINING) < 0) {
            BigDecimal maxAllowedQuantity = availableQuantity.subtract(MINIMUM_REMAINING);
            throw new PurchaseException("INSUFFICIENT_PRODUCT", 
                    String.format("Cannot withdraw all product. At least %s unit must remain on warehouse. Available: %s, maximum allowed withdrawal: %s",
                            MINIMUM_REMAINING, availableQuantity, maxAllowedQuantity));
        }
    }

    private record PriceCalculationResult(BigDecimal unitPrice, BigDecimal totalCost) {}

    private PriceCalculationResult calculatePriceAndRemoveProduct(
            @NonNull WarehouseWithdrawal warehouseWithdrawal,
            @NonNull WarehouseProductBalance balance,
            @NonNull BigDecimal quantity) {
        
        BigDecimal unitPrice;
        BigDecimal totalCost;
        
        if (warehouseWithdrawal.getVehicleId() != null) {
            unitPrice = balance.getAveragePriceEur();
            totalCost = calculateTotalCost(unitPrice, quantity);
            
            warehouseProductBalanceService.removeProductWithCost(
                    warehouseWithdrawal.getWarehouseId(),
                    warehouseWithdrawal.getProductId(),
                    quantity,
                    totalCost
            );
        } else {
            BigDecimal newAveragePrice = warehouseProductBalanceService.removeProduct(
                    warehouseWithdrawal.getWarehouseId(),
                    warehouseWithdrawal.getProductId(),
                    quantity
            );
            
            if (newAveragePrice == null) {
                throw new PurchaseException("INVALID_BALANCE", "Average price cannot be null after removal");
            }
            
            unitPrice = newAveragePrice.setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
            totalCost = calculateTotalCost(unitPrice, quantity);
        }
        
        return new PriceCalculationResult(unitPrice, totalCost);
    }

    private void setWithdrawalDateIfNeeded(@NonNull WarehouseWithdrawal warehouseWithdrawal) {
        if (warehouseWithdrawal.getWithdrawalDate() == null) {
            warehouseWithdrawal.setWithdrawalDate(LocalDate.now());
        }
    }

    private WarehouseWithdrawal findWithdrawalById(@NonNull Long id) {
        return warehouseWithdrawalRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Withdrawal not found: id={}", id);
                    return new PurchaseException("WITHDRAWAL_NOT_FOUND", 
                            String.format("Withdrawal with ID %d not found", id));
                });
    }

    private void validateProductChange(@NonNull WarehouseWithdrawal request, @NonNull WarehouseWithdrawal withdrawal) {
        if (request.getProductId() != null && !request.getProductId().equals(withdrawal.getProductId())) {
            throw new PurchaseException("PRODUCT_CHANGE_NOT_ALLOWED", 
                    "Cannot change product for existing withdrawal");
        }
    }

    private void handleQuantityIncrease(
            @NonNull WarehouseWithdrawal withdrawal,
            @NonNull BigDecimal delta,
            @NonNull BigDecimal unitPrice) {
        
        WarehouseProductBalance balance = validateAndGetBalance(
                withdrawal.getWarehouseId(), 
                withdrawal.getProductId());
        
        BigDecimal availableQuantity = balance.getQuantity();
        
        if (availableQuantity.compareTo(delta) < 0) {
            throw new PurchaseException("INSUFFICIENT_PRODUCT", 
                    String.format("Insufficient product on warehouse. Available: %s, requested additionally: %s",
                            availableQuantity, delta));
        }
        
        BigDecimal additionalQuantity = delta.setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE);
        BigDecimal additionalCost = calculateTotalCost(unitPrice, additionalQuantity);
        
        if (withdrawal.getVehicleId() != null) {
            warehouseProductBalanceService.removeProductWithCost(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    additionalQuantity,
                    additionalCost
            );
            vehicleService.addWithdrawalCost(withdrawal.getVehicleId(), additionalCost);
        } else {
            validateMinimumRemaining(availableQuantity, delta);
            
            warehouseProductBalanceService.removeProduct(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    additionalQuantity
            );
        }
    }

    private void handleQuantityDecrease(
            @NonNull WarehouseWithdrawal withdrawal,
            @NonNull BigDecimal quantityToReturn,
            @NonNull BigDecimal unitPrice) {
        
        BigDecimal scaledQuantity = quantityToReturn.setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE);
        BigDecimal costToReturn = calculateTotalCost(unitPrice, scaledQuantity);
        
        if (withdrawal.getVehicleId() != null) {
            warehouseProductBalanceService.addProduct(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    scaledQuantity,
                    costToReturn
            );
            vehicleService.subtractWithdrawalCost(withdrawal.getVehicleId(), costToReturn);
        } else {
            warehouseProductBalanceService.addProductQuantityOnly(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    scaledQuantity
            );
        }
    }

    private void updateWithdrawalFields(
            @NonNull WarehouseWithdrawal withdrawal,
            @NonNull WarehouseWithdrawal request,
            @NonNull BigDecimal newQuantity,
            @NonNull BigDecimal unitPrice) {
        
        if (request.getWithdrawalReason() != null) {
            if (request.getWithdrawalReason().getPurpose() != WithdrawalReason.Purpose.REMOVING) {
                throw new PurchaseException("INVALID_WITHDRAWAL_REASON", 
                        "Withdrawal reason must have REMOVING purpose");
            }
            withdrawal.setWithdrawalReason(request.getWithdrawalReason());
        }
        
        if (request.getDescription() != null) {
            withdrawal.setDescription(request.getDescription());
        }
        
        if (request.getWithdrawalDate() != null) {
            withdrawal.setWithdrawalDate(request.getWithdrawalDate());
        }
        
        withdrawal.setQuantity(newQuantity);
        withdrawal.setUnitPriceEur(unitPrice);
        withdrawal.setTotalCostEur(calculateTotalCost(unitPrice, newQuantity));
    }

    private WithdrawalDTO convertToDTO(@NonNull WarehouseWithdrawal withdrawal) {
        if (withdrawal.getQuantity() == null) {
            throw new PurchaseException("INVALID_WITHDRAWAL", "Withdrawal quantity cannot be null");
        }
        
        return WithdrawalDTO.builder()
                .id(withdrawal.getId())
                .productId(withdrawal.getProductId())
                .warehouseId(withdrawal.getWarehouseId())
                .userId(withdrawal.getUserId())
                .withdrawalReason(withdrawal.getWithdrawalReason())
                .quantity(withdrawal.getQuantity().doubleValue())
                .unitPriceEur(withdrawal.getUnitPriceEur())
                .totalCostEur(withdrawal.getTotalCostEur())
                .description(withdrawal.getDescription())
                .withdrawalDate(withdrawal.getWithdrawalDate())
                .createdAt(withdrawal.getCreatedAt())
                .build();
    }


    private BigDecimal resolveUnitPrice(@NonNull WarehouseWithdrawal withdrawal) {
        BigDecimal unitPrice = withdrawal.getUnitPriceEur();
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            return unitPrice.setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
        }

        BigDecimal quantity = withdrawal.getQuantity();
        BigDecimal totalCost = withdrawal.getTotalCostEur();
        if (quantity != null
                && quantity.compareTo(BigDecimal.ZERO) > 0
                && totalCost != null
                && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            return totalCost.divide(quantity, PRICE_SCALE, PRICE_ROUNDING_MODE);
        }

        throw new PurchaseException("WITHDRAWAL_PRICE_MISSING",
                String.format("Withdrawal %d does not contain price information", withdrawal.getId()));
    }

    private BigDecimal calculateTotalCost(@NonNull BigDecimal unitPrice, @NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
        }
        return unitPrice.multiply(quantity).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
    }

    private void restoreWithdrawalToWarehouse(@NonNull WarehouseWithdrawal withdrawal) {
        BigDecimal quantity = withdrawal.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal scaledQuantity = quantity.setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE);
        BigDecimal unitPrice = resolveUnitPrice(withdrawal);
        BigDecimal totalCost = calculateTotalCost(unitPrice, scaledQuantity);

        if (withdrawal.getVehicleId() != null) {
            warehouseProductBalanceService.addProduct(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    scaledQuantity,
                    totalCost
            );
            vehicleService.subtractWithdrawalCost(withdrawal.getVehicleId(), totalCost);
        } else {
            warehouseProductBalanceService.addProductQuantityOnly(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    scaledQuantity
            );
        }
    }
}
