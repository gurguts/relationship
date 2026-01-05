package org.example.purchaseservice.services.balance;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.AdjustmentType;
import org.example.purchaseservice.models.balance.WarehouseBalanceAdjustment;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.repositories.WarehouseBalanceAdjustmentRepository;
import org.example.purchaseservice.repositories.WarehouseProductBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseProductBalanceService implements IWarehouseProductBalanceService {
    
    private static final int PRICE_SCALE = 6;
    private static final int QUANTITY_SCALE = 2;
    private static final RoundingMode PRICE_ROUNDING_MODE = RoundingMode.CEILING;
    private static final RoundingMode QUANTITY_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    private final WarehouseProductBalanceRepository warehouseProductBalanceRepository;
    private final WarehouseBalanceAdjustmentRepository warehouseBalanceAdjustmentRepository;
    
    @Override
    @Transactional
    public WarehouseProductBalance addProduct(@NonNull Long warehouseId, @NonNull Long productId, 
                                               @NonNull BigDecimal quantity, @NonNull BigDecimal totalCost) {
        log.info("Adding product to warehouse balance: warehouseId={}, productId={}, quantity={}, totalCost={}", 
                warehouseId, productId, quantity, totalCost);
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Added quantity must be positive");
        }
        if (totalCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TOTAL_COST", "Added total cost must be non-negative");
        }
        
        WarehouseProductBalance balance = getOrCreateBalance(warehouseId, productId);
        
        BigDecimal currentTotalCost = balance.getTotalCostEur() != null ? balance.getTotalCostEur() : BigDecimal.ZERO;
        BigDecimal currentQuantity = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        
        BigDecimal newTotalCost = currentTotalCost.add(totalCost);
        BigDecimal newQuantity = currentQuantity.add(quantity);
        
        balance.setQuantity(newQuantity);
        balance.setTotalCostEur(newTotalCost);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            balance.setAveragePriceEur(calculateAveragePrice(newTotalCost, newQuantity));
        }
        
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        
        log.info("Warehouse balance updated: id={}, newQuantity={}, newTotalCost={}, newAveragePrice={}", 
                saved.getId(), saved.getQuantity(), saved.getTotalCostEur(), saved.getAveragePriceEur());
        
        return saved;
    }
    
    @Override
    @Transactional
    public BigDecimal removeProduct(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal quantity) {
        log.info("Removing product from warehouse balance: warehouseId={}, productId={}, quantity={}", 
                warehouseId, productId, quantity);
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Removed quantity must be positive");
        }
        
        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new PurchaseException("WAREHOUSE_BALANCE_NOT_FOUND", 
                        String.format("Warehouse balance not found: warehouseId=%d, productId=%d", 
                                warehouseId, productId)));
        
        validateSufficientQuantity(balance, quantity);
        
        BigDecimal currentAveragePrice = balance.getAveragePriceEur() != null ? balance.getAveragePriceEur() : BigDecimal.ZERO;
        BigDecimal currentQuantity = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        BigDecimal newQuantity = currentQuantity.subtract(quantity);
        
        balance.setQuantity(newQuantity);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            balance.setAveragePriceEur(BigDecimal.ZERO);
            balance.setTotalCostEur(BigDecimal.ZERO);
        } else {
            BigDecimal currentTotalCost = balance.getTotalCostEur() != null ? balance.getTotalCostEur() : BigDecimal.ZERO;
            balance.setAveragePriceEur(calculateAveragePrice(currentTotalCost, newQuantity));
        }
        
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        deleteBalanceIfEmpty(saved);
        
        if (saved.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            log.info("Warehouse balance updated: id={}, newQuantity={}, totalCost={}, averagePrice={}", 
                    saved.getId(), saved.getQuantity(), saved.getTotalCostEur(), saved.getAveragePriceEur());
        }
        
        return currentAveragePrice;
    }
    
    @Override
    @Transactional
    public void addProductQuantityOnly(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal quantity) {
        log.info("Adding product quantity only to warehouse balance: warehouseId={}, productId={}, quantity={}", 
                warehouseId, productId, quantity);
        
        WarehouseProductBalance balance = getOrCreateBalance(warehouseId, productId);
        
        BigDecimal currentQuantity = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        BigDecimal newQuantity = currentQuantity.add(quantity);
        balance.setQuantity(newQuantity);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotalCost = balance.getTotalCostEur() != null ? balance.getTotalCostEur() : BigDecimal.ZERO;
            balance.setAveragePriceEur(calculateAveragePrice(currentTotalCost, newQuantity));
        }
        
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        
        log.info("Warehouse balance updated (quantity only): id={}, newQuantity={}, totalCost={}, averagePrice={}", 
                saved.getId(), saved.getQuantity(), saved.getTotalCostEur(), saved.getAveragePriceEur());
    }
    
    @Override
    @Transactional
    public void removeProductWithCost(@NonNull Long warehouseId, @NonNull Long productId, 
                                      @NonNull BigDecimal quantity, @NonNull BigDecimal totalCost) {
        log.info("Removing product with specific cost from warehouse: warehouseId={}, productId={}, quantity={}, totalCost={}", 
                warehouseId, productId, quantity, totalCost);
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Removed quantity must be positive");
        }
        if (totalCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TOTAL_COST", "Removed total cost must be non-negative");
        }
        
        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new PurchaseException("WAREHOUSE_BALANCE_NOT_FOUND", 
                        String.format("Warehouse balance not found: warehouseId=%d, productId=%d", 
                                warehouseId, productId)));
        
        validateSufficientQuantity(balance, quantity);
        
        BigDecimal currentQuantity = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        BigDecimal currentTotalCost = balance.getTotalCostEur() != null ? balance.getTotalCostEur() : BigDecimal.ZERO;
        
        BigDecimal newQuantity = currentQuantity.subtract(quantity);
        BigDecimal newTotalCost = currentTotalCost.subtract(totalCost);
        
        if (newTotalCost.compareTo(BigDecimal.ZERO) < 0) {
            newTotalCost = BigDecimal.ZERO;
        }
        
        balance.setQuantity(newQuantity);
        balance.setTotalCostEur(newTotalCost);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            balance.setAveragePriceEur(calculateAveragePrice(newTotalCost, newQuantity));
        } else {
            balance.setAveragePriceEur(BigDecimal.ZERO);
            balance.setTotalCostEur(BigDecimal.ZERO);
        }
        
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        deleteBalanceIfEmpty(saved);
        
        if (saved.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            log.info("Warehouse balance updated: id={}, newQuantity={}, newTotalCost={}, newAveragePrice={}", 
                    saved.getId(), saved.getQuantity(), saved.getTotalCostEur(), saved.getAveragePriceEur());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public WarehouseProductBalance getBalance(@NonNull Long warehouseId, @NonNull Long productId) {
        return warehouseProductBalanceRepository.findByWarehouseIdAndProductId(warehouseId, productId).orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<WarehouseProductBalance> getWarehouseBalances(@NonNull Long warehouseId) {
        return warehouseProductBalanceRepository.findByWarehouseId(warehouseId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<WarehouseProductBalance> getProductBalances(@NonNull Long productId) {
        return warehouseProductBalanceRepository.findByProductId(productId);
    }
    
    @Override
    @Transactional
    public void adjustProductCost(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal costDelta) {
        log.info("Adjusting warehouse product cost: warehouseId={}, productId={}, costDelta={}",
                warehouseId, productId, costDelta);

        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new PurchaseException("WAREHOUSE_BALANCE_NOT_FOUND",
                        String.format("Warehouse balance not found: warehouseId=%d, productId=%d",
                                warehouseId, productId)));


        BigDecimal newTotalCost = balance.getTotalCostEur().add(costDelta);
        if (newTotalCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_COST_ADJUSTMENT", "Resulting total cost cannot be negative");
        }

        balance.setTotalCostEur(newTotalCost);

        BigDecimal currentQuantity = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        if (currentQuantity.compareTo(BigDecimal.ZERO) > 0) {
            balance.setAveragePriceEur(calculateAveragePrice(newTotalCost, currentQuantity));
        } else {
            balance.setTotalCostEur(BigDecimal.ZERO);
            balance.setAveragePriceEur(BigDecimal.ZERO);
        }

        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        deleteBalanceIfEmpty(saved);
        
        if (saved.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            log.info("Warehouse balance cost adjusted: id={}, totalCost={}, averagePrice={}",
                    saved.getId(), saved.getTotalCostEur(), saved.getAveragePriceEur());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEnoughProduct(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal requiredQuantity) {
        if (requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Required quantity must be positive");
        }
        
        WarehouseProductBalance balance = getBalance(warehouseId, productId);
        if (balance == null) {
            return false;
        }
        
        BigDecimal availableQuantity = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        return availableQuantity.compareTo(requiredQuantity) >= 0;
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAveragePrice(@NonNull Long warehouseId, @NonNull Long productId) {
        WarehouseProductBalance balance = getBalance(warehouseId, productId);
        return balance != null ? balance.getAveragePriceEur() : BigDecimal.ZERO;
    }
    
    @Override
    @Transactional
    public WarehouseProductBalance setInitialBalance(@NonNull Long warehouseId, @NonNull Long productId,
                                                      @NonNull BigDecimal initialQuantity, @NonNull BigDecimal averagePriceEur) {
        log.info("Setting initial warehouse balance: warehouseId={}, productId={}, quantity={}, avgPrice={}", 
                warehouseId, productId, initialQuantity, averagePriceEur);
        
        if (initialQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Initial quantity cannot be negative");
        }
        if (averagePriceEur.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_AVERAGE_PRICE", "Average price cannot be negative");
        }
        
        Optional<WarehouseProductBalance> existing = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId);
        
        if (existing.isPresent()) {
            throw new PurchaseException("BALANCE_ALREADY_EXISTS", 
                    String.format("Balance already exists for warehouse %d, product %d. Use update instead.", 
                            warehouseId, productId));
        }
        
        WarehouseProductBalance balance = new WarehouseProductBalance();
        balance.setWarehouseId(warehouseId);
        balance.setProductId(productId);
        balance.setQuantity(initialQuantity);
        balance.setAveragePriceEur(averagePriceEur);
        balance.setTotalCostEur(initialQuantity.multiply(averagePriceEur));
        
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        
        log.info("Initial warehouse balance set: id={}, quantity={}, avgPrice={}, totalCost={}", 
                saved.getId(), saved.getQuantity(), saved.getAveragePriceEur(), saved.getTotalCostEur());
        
        return saved;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<WarehouseProductBalance> getAllActiveBalances() {
        return warehouseProductBalanceRepository.findAllWithPositiveQuantity();
    }

    @Override
    @Transactional
    public WarehouseProductBalance updateBalance(@NonNull Long warehouseId,
                                                 @NonNull Long productId,
                                                 BigDecimal newQuantity,
                                                 BigDecimal newTotalCost,
                                                 Long userId,
                                                 String description) {
        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new PurchaseException("WAREHOUSE_BALANCE_NOT_FOUND",
                        String.format("Warehouse balance not found: warehouseId=%d, productId=%d",
                                warehouseId, productId)));

        BigDecimal previousQuantity = balance.getQuantity().setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE);
        BigDecimal previousTotalCost = balance.getTotalCostEur().setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);
        BigDecimal previousAverage = balance.getAveragePriceEur().setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);

        BigDecimal unitPrice = resolveUnitPrice(previousQuantity, previousTotalCost);

        AdjustmentUpdateResult updateResult = processAdjustmentUpdates(
                newQuantity, newTotalCost, previousQuantity, previousTotalCost, unitPrice);
        
        balance.setQuantity(updateResult.updatedQuantity());
        balance.setTotalCostEur(updateResult.updatedTotalCost());
        balance.setAveragePriceEur(updateResult.updatedAverage());

        WarehouseProductBalance savedBalance = warehouseProductBalanceRepository.save(balance);
        createBalanceAdjustment(warehouseId, productId, userId, description, previousQuantity, 
                previousTotalCost, previousAverage, updateResult);
        deleteBalanceIfEmpty(savedBalance);

        log.info("Warehouse balance updated: warehouseId={}, productId={}, quantity {} -> {}, totalCost {} -> {}",
                warehouseId, productId, previousQuantity, updateResult.updatedQuantity(), 
                previousTotalCost, updateResult.updatedTotalCost());

        return savedBalance;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseBalanceAdjustment> getBalanceAdjustments(@NonNull Long warehouseId, @NonNull Long productId) {
        return warehouseBalanceAdjustmentRepository
                .findByWarehouseIdAndProductIdOrderByCreatedAtDesc(warehouseId, productId);
    }

    private WarehouseProductBalance getOrCreateBalance(Long warehouseId, Long productId) {
        return warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseGet(() -> createNewBalance(warehouseId, productId));
    }
    
    private WarehouseProductBalance createNewBalance(Long warehouseId, Long productId) {
        WarehouseProductBalance newBalance = new WarehouseProductBalance();
        newBalance.setWarehouseId(warehouseId);
        newBalance.setProductId(productId);
        newBalance.setQuantity(BigDecimal.ZERO);
        newBalance.setTotalCostEur(BigDecimal.ZERO);
        newBalance.setAveragePriceEur(BigDecimal.ZERO);
        return newBalance;
    }
    
    private BigDecimal calculateAveragePrice(BigDecimal totalCost, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (totalCost == null) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(quantity, PRICE_SCALE, PRICE_ROUNDING_MODE);
    }
    
    private void deleteBalanceIfEmpty(WarehouseProductBalance balance) {
        if (balance.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            warehouseProductBalanceRepository.delete(balance);
            log.info("Warehouse balance deleted (quantity=0): id={}", balance.getId());
        }
    }
    
    private AdjustmentUpdateResult processAdjustmentUpdates(BigDecimal newQuantity, BigDecimal newTotalCost,
                                                           BigDecimal previousQuantity, BigDecimal previousTotalCost,
                                                           BigDecimal unitPrice) {
        AdjustmentType adjustmentType = null;
        BigDecimal updatedQuantity = previousQuantity;
        BigDecimal updatedTotalCost = previousTotalCost;

        if (newQuantity != null) {
            updatedQuantity = newQuantity.setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE);
            if (updatedQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be negative");
            }
            if (newTotalCost == null) {
                if (updatedQuantity.compareTo(BigDecimal.ZERO) == 0) {
                    updatedTotalCost = BigDecimal.ZERO.setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);
                } else {
                    updatedTotalCost = unitPrice.multiply(updatedQuantity).setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);
                }
            }
            adjustmentType = AdjustmentType.QUANTITY;
        }

        if (newTotalCost != null) {
            BigDecimal total = newTotalCost.setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                throw new PurchaseException("INVALID_TOTAL_COST", "Total cost cannot be negative");
            }
            updatedTotalCost = total;
            adjustmentType = adjustmentType == null
                    ? AdjustmentType.TOTAL_COST
                    : AdjustmentType.BOTH;
        }

        if (adjustmentType == null) {
            throw new PurchaseException("NO_CHANGES", "No changes were provided");
        }

        BigDecimal updatedAverage;
        if (updatedQuantity.compareTo(BigDecimal.ZERO) > 0) {
            updatedAverage = calculateAveragePrice(updatedTotalCost, updatedQuantity);
        } else {
            updatedAverage = BigDecimal.ZERO.setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);
            updatedTotalCost = BigDecimal.ZERO.setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);
        }

        return new AdjustmentUpdateResult(updatedQuantity, updatedTotalCost, updatedAverage, adjustmentType);
    }
    
    private void createBalanceAdjustment(Long warehouseId, Long productId, Long userId, String description,
                                        BigDecimal previousQuantity, BigDecimal previousTotalCost, BigDecimal previousAverage,
                                        AdjustmentUpdateResult updateResult) {
        WarehouseBalanceAdjustment adjustment = new WarehouseBalanceAdjustment();
        adjustment.setWarehouseId(warehouseId);
        adjustment.setProductId(productId);
        adjustment.setPreviousQuantity(previousQuantity);
        adjustment.setNewQuantity(updateResult.updatedQuantity());
        adjustment.setPreviousTotalCostEur(previousTotalCost);
        adjustment.setNewTotalCostEur(updateResult.updatedTotalCost());
        adjustment.setPreviousAveragePriceEur(previousAverage);
        adjustment.setNewAveragePriceEur(updateResult.updatedAverage());
        adjustment.setAdjustmentType(updateResult.adjustmentType());
        adjustment.setDescription(description);
        adjustment.setUserId(userId);
        warehouseBalanceAdjustmentRepository.save(adjustment);
    }
    
    private void validateSufficientQuantity(WarehouseProductBalance balance, BigDecimal requiredQuantity) {
        BigDecimal availableQuantity = balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
        if (requiredQuantity.compareTo(availableQuantity) > 0) {
            throw new PurchaseException("INSUFFICIENT_QUANTITY", 
                    String.format("Cannot remove more than available quantity. Available: %s, trying to remove: %s", 
                            availableQuantity, requiredQuantity));
        }
    }
    
    private BigDecimal resolveUnitPrice(BigDecimal quantity, BigDecimal totalCost) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);
        }
        if (totalCost == null) {
            return BigDecimal.ZERO.setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);
        }
        return totalCost.divide(quantity, PRICE_SCALE, PRICE_ROUNDING_MODE);
    }
    
    private record AdjustmentUpdateResult(
            BigDecimal updatedQuantity,
            BigDecimal updatedTotalCost,
            BigDecimal updatedAverage,
            AdjustmentType adjustmentType
    ) {}
}


