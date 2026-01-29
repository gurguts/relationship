package org.example.purchaseservice.services.balance;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.WarehouseBalanceAdjustment;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.services.balance.WarehouseBalanceUpdateRecords.AdjustmentUpdateResult;
import org.example.purchaseservice.repositories.WarehouseBalanceAdjustmentRepository;
import org.example.purchaseservice.repositories.WarehouseProductBalanceRepository;
import org.example.purchaseservice.services.impl.IWarehouseProductBalanceService;
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
    private static final RoundingMode QUANTITY_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    private final WarehouseProductBalanceRepository warehouseProductBalanceRepository;
    private final WarehouseBalanceAdjustmentRepository warehouseBalanceAdjustmentRepository;
    private final WarehouseProductBalanceValidator validator;
    private final WarehouseProductBalanceCalculator calculator;
    private final WarehouseProductBalanceHelper helper;
    private final WarehouseBalanceAdjustmentService adjustmentService;
    private final WarehouseBalanceUpdateProcessor updateProcessor;
    
    @Override
    @Transactional
    public void addProduct(@NonNull Long warehouseId, @NonNull Long productId,
                           @NonNull BigDecimal quantity, @NonNull BigDecimal totalCost) {
        log.info("Adding product to warehouse balance: warehouseId={}, productId={}, quantity={}, totalCost={}", 
                warehouseId, productId, quantity, totalCost);
        
        validator.validateQuantityPositive(quantity, "Added");
        validator.validateTotalCostNonNegative(totalCost, "Added");
        
        WarehouseProductBalance balance = helper.getOrCreateBalance(warehouseId, productId);
        
        BigDecimal currentTotalCost = helper.getSafeTotalCost(balance);
        BigDecimal currentQuantity = helper.getSafeQuantity(balance);
        
        BigDecimal newTotalCost = currentTotalCost.add(totalCost);
        BigDecimal newQuantity = currentQuantity.add(quantity);
        
        balance.setQuantity(newQuantity);
        balance.setTotalCostEur(newTotalCost);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            balance.setAveragePriceEur(calculator.calculateAveragePrice(newTotalCost, newQuantity));
        }
        
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        
        logBalanceUpdated(saved);
    }
    
    @Override
    @Transactional
    public BigDecimal removeProduct(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal quantity) {
        log.info("Removing product from warehouse balance: warehouseId={}, productId={}, quantity={}", 
                warehouseId, productId, quantity);
        
        validator.validateQuantityPositive(quantity, "Removed");
        
        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new PurchaseException("WAREHOUSE_BALANCE_NOT_FOUND", 
                        String.format("Warehouse balance not found: warehouseId=%d, productId=%d", 
                                warehouseId, productId)));
        
        validator.validateSufficientQuantity(balance, quantity, helper);
        
        BigDecimal currentAveragePrice = helper.getSafeAveragePrice(balance);
        BigDecimal currentQuantity = helper.getSafeQuantity(balance);
        BigDecimal newQuantity = currentQuantity.subtract(quantity);
        
        balance.setQuantity(newQuantity);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            helper.resetBalanceToZero(balance);
        } else {
            BigDecimal currentTotalCost = helper.getSafeTotalCost(balance);
            balance.setAveragePriceEur(calculator.calculateAveragePrice(currentTotalCost, newQuantity));
        }
        
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        helper.deleteBalanceIfEmpty(saved);
        
        if (saved.getQuantity() != null && saved.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            logBalanceUpdated(saved);
        }
        
        return currentAveragePrice;
    }
    
    @Override
    @Transactional
    public void addProductQuantityOnly(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal quantity) {
        log.info("Adding product quantity only to warehouse balance: warehouseId={}, productId={}, quantity={}", 
                warehouseId, productId, quantity);
        
        validator.validateQuantityPositive(quantity, "Added");
        
        WarehouseProductBalance balance = helper.getOrCreateBalance(warehouseId, productId);
        
        BigDecimal currentQuantity = helper.getSafeQuantity(balance);
        BigDecimal newQuantity = currentQuantity.add(quantity);
        balance.setQuantity(newQuantity);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotalCost = helper.getSafeTotalCost(balance);
            balance.setAveragePriceEur(calculator.calculateAveragePrice(currentTotalCost, newQuantity));
        }
        
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        
        logBalanceUpdated(saved);
    }
    
    @Override
    @Transactional
    public void removeProductWithCost(@NonNull Long warehouseId, @NonNull Long productId, 
                                      @NonNull BigDecimal quantity, @NonNull BigDecimal totalCost) {
        log.info("Removing product with specific cost from warehouse: warehouseId={}, productId={}, quantity={}, totalCost={}", 
                warehouseId, productId, quantity, totalCost);
        
        validator.validateQuantityPositive(quantity, "Removed");
        validator.validateTotalCostNonNegative(totalCost, "Removed");
        
        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new PurchaseException("WAREHOUSE_BALANCE_NOT_FOUND", 
                        String.format("Warehouse balance not found: warehouseId=%d, productId=%d", 
                                warehouseId, productId)));
        
        validator.validateSufficientQuantity(balance, quantity, helper);
        
        BigDecimal currentQuantity = helper.getSafeQuantity(balance);
        BigDecimal currentTotalCost = helper.getSafeTotalCost(balance);
        
        BigDecimal newQuantity = currentQuantity.subtract(quantity);
        BigDecimal newTotalCost = currentTotalCost.subtract(totalCost);
        
        if (newTotalCost.compareTo(BigDecimal.ZERO) < 0) {
            newTotalCost = BigDecimal.ZERO;
        }
        
        balance.setQuantity(newQuantity);
        balance.setTotalCostEur(newTotalCost);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            balance.setAveragePriceEur(calculator.calculateAveragePrice(newTotalCost, newQuantity));
        } else {
            helper.resetBalanceToZero(balance);
        }
        
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        helper.deleteBalanceIfEmpty(saved);
        
        if (saved.getQuantity() != null && saved.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            logBalanceUpdated(saved);
        }
    }
    
    private void logBalanceUpdated(@NonNull WarehouseProductBalance balance) {
        log.info("Warehouse balance updated: id={}, newQuantity={}, newTotalCost={}, newAveragePrice={}", 
                balance.getId(), balance.getQuantity(), balance.getTotalCostEur(), balance.getAveragePriceEur());
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
    @Transactional
    public void adjustProductCost(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal costDelta) {
        log.info("Adjusting warehouse product cost: warehouseId={}, productId={}, costDelta={}",
                warehouseId, productId, costDelta);

        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new PurchaseException("WAREHOUSE_BALANCE_NOT_FOUND",
                        String.format("Warehouse balance not found: warehouseId=%d, productId=%d",
                                warehouseId, productId)));

        BigDecimal currentTotalCost = helper.getSafeTotalCost(balance);
        BigDecimal newTotalCost = currentTotalCost.add(costDelta);
        validator.validateCostAdjustmentResult(newTotalCost);

        BigDecimal currentQuantity = helper.getSafeQuantity(balance);
        if (currentQuantity.compareTo(BigDecimal.ZERO) > 0) {
            balance.setTotalCostEur(newTotalCost);
            balance.setAveragePriceEur(calculator.calculateAveragePrice(newTotalCost, currentQuantity));
        } else {
            helper.resetBalanceToZero(balance);
        }

        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        helper.deleteBalanceIfEmpty(saved);
        
        if (saved.getQuantity() != null && saved.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            log.info("Warehouse balance cost adjusted: id={}, totalCost={}, averagePrice={}",
                    saved.getId(), saved.getTotalCostEur(), saved.getAveragePriceEur());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEnoughProduct(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal requiredQuantity) {
        validator.validateQuantityPositive(requiredQuantity, "Required");
        
        WarehouseProductBalance balance = getBalance(warehouseId, productId);
        if (balance == null) {
            return false;
        }
        
        BigDecimal availableQuantity = helper.getSafeQuantity(balance);
        return availableQuantity.compareTo(requiredQuantity) >= 0;
    }

    @Override
    @Transactional
    public WarehouseProductBalance setInitialBalance(@NonNull Long warehouseId, @NonNull Long productId,
                                                      @NonNull BigDecimal initialQuantity, @NonNull BigDecimal averagePriceEur) {
        log.info("Setting initial warehouse balance: warehouseId={}, productId={}, quantity={}, avgPrice={}", 
                warehouseId, productId, initialQuantity, averagePriceEur);
        
        validator.validateQuantityNonNegative(initialQuantity);
        validator.validateAveragePriceNonNegative(averagePriceEur);
        
        Optional<WarehouseProductBalance> existing = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId);
        
        validator.validateBalanceDoesNotExist(existing.isPresent(), warehouseId, productId);
        
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

        BigDecimal previousQuantity = helper.getSafeQuantity(balance).setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE);
        BigDecimal previousTotalCost = helper.getSafeTotalCost(balance).setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);
        BigDecimal previousAverage = helper.getSafeAveragePrice(balance).setScale(PRICE_SCALE, QUANTITY_ROUNDING_MODE);

        BigDecimal unitPrice = calculator.resolveUnitPrice(previousQuantity, previousTotalCost);

        AdjustmentUpdateResult updateResult = updateProcessor.processAdjustmentUpdates(
                newQuantity, newTotalCost, previousQuantity, previousTotalCost, unitPrice);
        
        balance.setQuantity(updateResult.updatedQuantity());
        balance.setTotalCostEur(updateResult.updatedTotalCost());
        balance.setAveragePriceEur(updateResult.updatedAverage());

        WarehouseProductBalance savedBalance = warehouseProductBalanceRepository.save(balance);
        adjustmentService.createBalanceAdjustment(warehouseId, productId, userId, description, previousQuantity, 
                previousTotalCost, previousAverage, updateResult);
        helper.deleteBalanceIfEmpty(savedBalance);

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
}
