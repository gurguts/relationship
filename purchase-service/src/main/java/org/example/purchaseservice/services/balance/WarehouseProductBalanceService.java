package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
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
public class WarehouseProductBalanceService {
    
    private final WarehouseProductBalanceRepository warehouseProductBalanceRepository;
    private final WarehouseBalanceAdjustmentRepository warehouseBalanceAdjustmentRepository;
    
    /**
     * Add product to warehouse (when receiving from driver)
     * @param totalCost TOTAL cost of all received product (NOT unit price)
     */
    @Transactional
    public WarehouseProductBalance addProduct(Long warehouseId, Long productId, 
                                               BigDecimal quantity, BigDecimal totalCost) {
        log.info("Adding product to warehouse balance: warehouseId={}, productId={}, quantity={}, totalCost={}", 
                warehouseId, productId, quantity, totalCost);
        
        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseGet(() -> {
                    WarehouseProductBalance newBalance = new WarehouseProductBalance();
                    newBalance.setWarehouseId(warehouseId);
                    newBalance.setProductId(productId);
                    return newBalance;
                });
        
        balance.addProduct(quantity, totalCost);
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        
        log.info("Warehouse balance updated: id={}, newQuantity={}, newTotalCost={}, newAveragePrice={}", 
                saved.getId(), saved.getQuantity(), saved.getTotalCostUah(), saved.getAveragePriceUah());
        
        return saved;
    }
    
    /**
     * Remove product from warehouse (during withdrawal)
     * @return average price of the product for withdrawal cost calculation
     */
    @Transactional
    public BigDecimal removeProduct(Long warehouseId, Long productId, BigDecimal quantity) {
        log.info("Removing product from warehouse balance: warehouseId={}, productId={}, quantity={}", 
                warehouseId, productId, quantity);
        
        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new PurchaseException("WAREHOUSE_BALANCE_NOT_FOUND", 
                        String.format("Warehouse balance not found: warehouseId=%d, productId=%d", 
                                warehouseId, productId)));
        
        BigDecimal averagePrice = balance.removeProduct(quantity);
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        
        // Delete record if quantity becomes 0
        if (saved.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            warehouseProductBalanceRepository.delete(saved);
            log.info("Warehouse balance deleted (quantity=0): id={}", saved.getId());
        } else {
            log.info("Warehouse balance updated: id={}, newQuantity={}, averagePrice={}", 
                    saved.getId(), saved.getQuantity(), saved.getAveragePriceUah());
        }
        
        return averagePrice;
    }
    
    /**
     * Remove product with specific cost from warehouse (for product movements/transfers)
     * This allows removing product at a specific price different from average warehouse price
     * @param quantity quantity to remove
     * @param totalCost total cost to remove (quantity * specified unit price)
     */
    @Transactional
    public void removeProductWithCost(Long warehouseId, Long productId, BigDecimal quantity, BigDecimal totalCost) {
        log.info("Removing product with specific cost from warehouse: warehouseId={}, productId={}, quantity={}, totalCost={}", 
                warehouseId, productId, quantity, totalCost);
        
        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new PurchaseException("WAREHOUSE_BALANCE_NOT_FOUND", 
                        String.format("Warehouse balance not found: warehouseId=%d, productId=%d", 
                                warehouseId, productId)));
        
        balance.removeProductWithCost(quantity, totalCost);
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        
        // Delete record if quantity becomes 0
        if (saved.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            warehouseProductBalanceRepository.delete(saved);
            log.info("Warehouse balance deleted (quantity=0): id={}", saved.getId());
        } else {
            log.info("Warehouse balance updated: id={}, newQuantity={}, newTotalCost={}, newAveragePrice={}", 
                    saved.getId(), saved.getQuantity(), saved.getTotalCostUah(), saved.getAveragePriceUah());
        }
    }
    
    /**
     * Get balance for specific product on warehouse
     */
    public WarehouseProductBalance getBalance(Long warehouseId, Long productId) {
        return warehouseProductBalanceRepository.findByWarehouseIdAndProductId(warehouseId, productId).orElse(null);
    }
    
    /**
     * Get all balances for specific warehouse
     */
    public List<WarehouseProductBalance> getWarehouseBalances(Long warehouseId) {
        return warehouseProductBalanceRepository.findByWarehouseId(warehouseId);
    }
    
    /**
     * Get all balances for specific product (across all warehouses)
     */
    public List<WarehouseProductBalance> getProductBalances(Long productId) {
        return warehouseProductBalanceRepository.findByProductId(productId);
    }
    
    @Transactional
    public void adjustProductCost(Long warehouseId, Long productId, BigDecimal costDelta) {
        log.info("Adjusting warehouse product cost: warehouseId={}, productId={}, costDelta={}",
                warehouseId, productId, costDelta);

        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new PurchaseException("WAREHOUSE_BALANCE_NOT_FOUND",
                        String.format("Warehouse balance not found: warehouseId=%d, productId=%d",
                                warehouseId, productId)));

        try {
            balance.adjustTotalCost(costDelta);
        } catch (IllegalArgumentException ex) {
            throw new PurchaseException("INVALID_COST_ADJUSTMENT", ex.getMessage());
        }

        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);

        if (saved.getQuantity().compareTo(BigDecimal.ZERO) == 0 && saved.getTotalCostUah().compareTo(BigDecimal.ZERO) == 0) {
            warehouseProductBalanceRepository.delete(saved);
            log.info("Warehouse balance deleted (quantity=0) after cost adjustment: id={}", saved.getId());
        } else {
            log.info("Warehouse balance cost adjusted: id={}, totalCost={}, averagePrice={}",
                    saved.getId(), saved.getTotalCostUah(), saved.getAveragePriceUah());
        }
    }

    /**
     * Check if warehouse has enough product
     */
    public boolean hasEnoughProduct(Long warehouseId, Long productId, BigDecimal requiredQuantity) {
        WarehouseProductBalance balance = getBalance(warehouseId, productId);
        if (balance == null) {
            return false;
        }
        return balance.getQuantity().compareTo(requiredQuantity) >= 0;
    }
    
    /**
     * Get average price of product on warehouse
     */
    public BigDecimal getAveragePrice(Long warehouseId, Long productId) {
        WarehouseProductBalance balance = getBalance(warehouseId, productId);
        return balance != null ? balance.getAveragePriceUah() : BigDecimal.ZERO;
    }
    
    /**
     * Set initial warehouse balance (for migration or manual initialization)
     * Use this when warehouse already has products but system is being set up
     */
    @Transactional
    public WarehouseProductBalance setInitialBalance(Long warehouseId, Long productId,
                                                      BigDecimal initialQuantity, BigDecimal averagePriceUah) {
        log.info("Setting initial warehouse balance: warehouseId={}, productId={}, quantity={}, avgPrice={}", 
                warehouseId, productId, initialQuantity, averagePriceUah);
        
        // Check if balance already exists
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
        balance.setAveragePriceUah(averagePriceUah);
        balance.setTotalCostUah(initialQuantity.multiply(averagePriceUah));
        
        WarehouseProductBalance saved = warehouseProductBalanceRepository.save(balance);
        
        log.info("Initial warehouse balance set: id={}, quantity={}, avgPrice={}, totalCost={}", 
                saved.getId(), saved.getQuantity(), saved.getAveragePriceUah(), saved.getTotalCostUah());
        
        return saved;
    }
    
    /**
     * Get all balances with quantity > 0
     */
    public List<WarehouseProductBalance> getAllActiveBalances() {
        return warehouseProductBalanceRepository.findAllWithPositiveQuantity();
    }

    @Transactional
    public WarehouseProductBalance updateBalance(Long warehouseId,
                                                 Long productId,
                                                 BigDecimal newQuantity,
                                                 BigDecimal newTotalCost,
                                                 Long userId,
                                                 String description) {
        WarehouseProductBalance balance = warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new PurchaseException("WAREHOUSE_BALANCE_NOT_FOUND",
                        String.format("Warehouse balance not found: warehouseId=%d, productId=%d",
                                warehouseId, productId)));

        BigDecimal previousQuantity = balance.getQuantity().setScale(2, RoundingMode.HALF_UP);
        BigDecimal previousTotalCost = balance.getTotalCostUah().setScale(6, RoundingMode.HALF_UP);
        BigDecimal previousAverage = balance.getAveragePriceUah().setScale(6, RoundingMode.HALF_UP);

        WarehouseBalanceAdjustment.AdjustmentType adjustmentType = null;

        BigDecimal updatedQuantity = previousQuantity;
        BigDecimal updatedTotalCost = previousTotalCost;

        BigDecimal unitPrice = resolveUnitPrice(previousQuantity, previousTotalCost);

        if (newQuantity != null) {
            updatedQuantity = newQuantity.setScale(2, RoundingMode.HALF_UP);
            if (updatedQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be negative");
            }
            if (newTotalCost == null) {
                if (updatedQuantity.compareTo(BigDecimal.ZERO) == 0) {
                    updatedTotalCost = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
                } else {
                    updatedTotalCost = unitPrice.multiply(updatedQuantity).setScale(6, RoundingMode.HALF_UP);
                }
            }
            adjustmentType = WarehouseBalanceAdjustment.AdjustmentType.QUANTITY;
        }

        if (newTotalCost != null) {
            BigDecimal total = newTotalCost.setScale(6, RoundingMode.HALF_UP);
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                throw new PurchaseException("INVALID_TOTAL_COST", "Total cost cannot be negative");
            }
            updatedTotalCost = total;
            adjustmentType = adjustmentType == null
                    ? WarehouseBalanceAdjustment.AdjustmentType.TOTAL_COST
                    : WarehouseBalanceAdjustment.AdjustmentType.BOTH;
        }

        if (adjustmentType == null) {
            throw new PurchaseException("NO_CHANGES", "No changes were provided");
        }

        BigDecimal updatedAverage;
        if (updatedQuantity.compareTo(BigDecimal.ZERO) > 0) {
            updatedAverage = updatedTotalCost.divide(updatedQuantity, 6, RoundingMode.HALF_UP);
        } else {
            updatedAverage = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
            updatedTotalCost = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }

        balance.setQuantity(updatedQuantity);
        balance.setTotalCostUah(updatedTotalCost);
        balance.setAveragePriceUah(updatedAverage);

        WarehouseProductBalance savedBalance = warehouseProductBalanceRepository.save(balance);

        WarehouseBalanceAdjustment adjustment = new WarehouseBalanceAdjustment();
        adjustment.setWarehouseId(warehouseId);
        adjustment.setProductId(productId);
        adjustment.setPreviousQuantity(previousQuantity);
        adjustment.setNewQuantity(updatedQuantity);
        adjustment.setPreviousTotalCostUah(previousTotalCost);
        adjustment.setNewTotalCostUah(updatedTotalCost);
        adjustment.setPreviousAveragePriceUah(previousAverage);
        adjustment.setNewAveragePriceUah(updatedAverage);
        adjustment.setAdjustmentType(adjustmentType);
        adjustment.setDescription(description);
        adjustment.setUserId(userId);
        warehouseBalanceAdjustmentRepository.save(adjustment);

        if (updatedQuantity.compareTo(BigDecimal.ZERO) == 0) {
            warehouseProductBalanceRepository.delete(savedBalance);
            log.info("Warehouse balance deleted after manual update (quantity=0): warehouseId={}, productId={}",
                    warehouseId, productId);
        }

        log.info("Warehouse balance updated: warehouseId={}, productId={}, quantity {} -> {}, totalCost {} -> {}",
                warehouseId, productId, previousQuantity, updatedQuantity, previousTotalCost, updatedTotalCost);

        return savedBalance;
    }

    @Transactional(readOnly = true)
    public List<WarehouseBalanceAdjustment> getBalanceAdjustments(Long warehouseId, Long productId) {
        return warehouseBalanceAdjustmentRepository
                .findByWarehouseIdAndProductIdOrderByCreatedAtDesc(warehouseId, productId);
    }

    private BigDecimal resolveUnitPrice(BigDecimal quantity, BigDecimal totalCost) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        if (totalCost == null) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return totalCost.divide(quantity, 6, RoundingMode.HALF_UP);
    }
}


