package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.repositories.DriverProductBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverProductBalanceService {
    
    private final DriverProductBalanceRepository driverProductBalanceRepository;
    
    /**
     * Add product to driver balance (when purchase is created)
     */
    @Transactional
    public DriverProductBalance addProduct(Long driverId, Long productId, 
                                            BigDecimal quantity, BigDecimal totalPriceEur) {
        log.info("Adding product to driver balance: driverId={}, productId={}, quantity={}, totalPriceEur={}", 
                driverId, productId, quantity, totalPriceEur);
        
        DriverProductBalance balance = driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseGet(() -> {
                    DriverProductBalance newBalance = new DriverProductBalance();
                    newBalance.setDriverId(driverId);
                    newBalance.setProductId(productId);
                    return newBalance;
                });
        
        balance.addProduct(quantity, totalPriceEur);
        DriverProductBalance saved = driverProductBalanceRepository.save(balance);
        
        log.info("Driver balance updated: id={}, newQuantity={}, newAveragePrice={}, totalCost={}", 
                saved.getId(), saved.getQuantity(), saved.getAveragePriceEur(), saved.getTotalCostEur());
        
        return saved;
    }
    
    /**
     * Remove product from driver balance (when purchase is deleted or warehouse entry is created)
     * @param totalPriceEur total price in EUR of the specific purchase being removed
     */
    @Transactional
    public DriverProductBalance removeProduct(Long driverId, Long productId, BigDecimal quantity, BigDecimal totalPriceEur) {
        log.info("Removing product from driver balance: driverId={}, productId={}, quantity={}, totalPrice={}", 
                driverId, productId, quantity, totalPriceEur);
        
        DriverProductBalance balance = driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseThrow(() -> new PurchaseException("BALANCE_NOT_FOUND", 
                        String.format("Driver balance not found: driverId=%d, productId=%d", driverId, productId)));
        
        balance.removeProduct(quantity, totalPriceEur);
        DriverProductBalance saved = driverProductBalanceRepository.save(balance);
        
        // Delete record if quantity becomes 0
        if (saved.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            driverProductBalanceRepository.delete(saved);
            log.info("Driver balance deleted (quantity=0): id={}", saved.getId());
            return null;
        }
        
        log.info("Driver balance updated: id={}, newQuantity={}, newAveragePrice={}, totalCost={}", 
                saved.getId(), saved.getQuantity(), saved.getAveragePriceEur(), saved.getTotalCostEur());
        
        return saved;
    }
    
    /**
     * Update balance when purchase is modified
     */
    @Transactional
    public DriverProductBalance updateFromPurchaseChange(Long driverId, Long productId,
                                                          BigDecimal oldQuantity, BigDecimal oldTotalPrice,
                                                          BigDecimal newQuantity, BigDecimal newTotalPrice) {
        log.info("Updating driver balance from purchase change: driverId={}, productId={}", driverId, productId);
        
        DriverProductBalance balance = driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseGet(() -> {
                    DriverProductBalance newBalance = new DriverProductBalance();
                    newBalance.setDriverId(driverId);
                    newBalance.setProductId(productId);
                    return newBalance;
                });
        
        balance.updateFromPurchaseChange(oldQuantity, oldTotalPrice, newQuantity, newTotalPrice);
        DriverProductBalance saved = driverProductBalanceRepository.save(balance);
        
        // Delete record if quantity becomes 0
        if (saved.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            driverProductBalanceRepository.delete(saved);
            log.info("Driver balance deleted after update (quantity=0): id={}", saved.getId());
            return null;
        }
        
        log.info("Driver balance updated: id={}, newQuantity={}, newAveragePrice={}, totalCost={}", 
                saved.getId(), saved.getQuantity(), saved.getAveragePriceEur(), saved.getTotalCostEur());
        
        return saved;
    }
    
    /**
     * Get driver balance for specific product
     */
    @Transactional(readOnly = true)
    public DriverProductBalance getBalance(Long driverId, Long productId) {
        return driverProductBalanceRepository.findByDriverIdAndProductId(driverId, productId).orElse(null);
    }
    
    /**
     * Get all balances for specific driver
     */
    @Transactional(readOnly = true)
    public List<DriverProductBalance> getDriverBalances(Long driverId) {
        return driverProductBalanceRepository.findByDriverId(driverId);
    }
    
    /**
     * Get all balances for specific product (across all drivers)
     */
    @Transactional(readOnly = true)
    public List<DriverProductBalance> getProductBalances(Long productId) {
        return driverProductBalanceRepository.findByProductId(productId);
    }
    
    /**
     * Check if driver has enough product
     */
    @Transactional(readOnly = true)
    public boolean hasEnoughProduct(Long driverId, Long productId, BigDecimal requiredQuantity) {
        DriverProductBalance balance = getBalance(driverId, productId);
        if (balance == null) {
            return false;
        }
        return balance.getQuantity().compareTo(requiredQuantity) >= 0;
    }
    
    /**
     * Get all balances with quantity > 0
     */
    @Transactional(readOnly = true)
    public List<DriverProductBalance> getAllActiveBalances() {
        return driverProductBalanceRepository.findAllWithPositiveQuantity();
    }
    
    @Transactional
    public DriverProductBalance updateTotalCostEur(Long driverId, Long productId, BigDecimal newTotalCostEur) {
        log.info("Updating total cost EUR: driverId={}, productId={}, newTotalCostEur={}", 
                driverId, productId, newTotalCostEur);
        
        DriverProductBalance balance = driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseThrow(() -> new PurchaseException("BALANCE_NOT_FOUND", 
                        String.format("Driver balance not found: driverId=%d, productId=%d", driverId, productId)));
        
        balance.updateTotalCostEur(newTotalCostEur);
        DriverProductBalance saved = driverProductBalanceRepository.save(balance);
        
        log.info("Driver balance total cost updated: id={}, newTotalCost={}, newAveragePrice={}", 
                saved.getId(), saved.getTotalCostEur(), saved.getAveragePriceEur());
        
        return saved;
    }
}

