package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.repositories.DriverProductBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverProductBalanceService {
    
    private final DriverProductBalanceRepository driverProductBalanceRepository;
    
    @Transactional
    public DriverProductBalance addProduct(Long driverId, Long productId, 
                                            BigDecimal quantity, BigDecimal totalPriceEur) {
        log.info("Adding product to driver balance: driverId={}, productId={}, quantity={}, totalPriceEur={}", 
                driverId, productId, quantity, totalPriceEur);
        
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Added quantity must be positive");
        }
        if (totalPriceEur == null || totalPriceEur.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total price must be non-negative");
        }
        
        DriverProductBalance balance = driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseGet(() -> {
                    DriverProductBalance newBalance = new DriverProductBalance();
                    newBalance.setDriverId(driverId);
                    newBalance.setProductId(productId);
                    return newBalance;
                });
        
        BigDecimal newTotalCost = balance.getTotalCostEur().add(totalPriceEur);
        BigDecimal newQuantity = balance.getQuantity().add(quantity);
        
        balance.setQuantity(newQuantity);
        balance.setTotalCostEur(newTotalCost);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            balance.setAveragePriceEur(newTotalCost.divide(newQuantity, 6, RoundingMode.CEILING));
        }
        
        DriverProductBalance saved = driverProductBalanceRepository.save(balance);
        
        log.info("Driver balance updated: id={}, newQuantity={}, newAveragePrice={}, totalCost={}", 
                saved.getId(), saved.getQuantity(), saved.getAveragePriceEur(), saved.getTotalCostEur());
        
        return saved;
    }
    
    @Transactional
    public DriverProductBalance removeProduct(Long driverId, Long productId, BigDecimal quantity, BigDecimal totalPriceEur) {
        log.info("Removing product from driver balance: driverId={}, productId={}, quantity={}, totalPrice={}", 
                driverId, productId, quantity, totalPriceEur);
        
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Removed quantity must be positive");
        }
        if (totalPriceEur == null || totalPriceEur.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total price of removed purchase must be non-negative");
        }
        
        DriverProductBalance balance = driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseThrow(() -> new PurchaseException("BALANCE_NOT_FOUND", 
                        String.format("Driver balance not found: driverId=%d, productId=%d", driverId, productId)));
        
        if (quantity.compareTo(balance.getQuantity()) > 0) {
            throw new IllegalArgumentException("Cannot remove more than available quantity. Available: " 
                    + balance.getQuantity() + ", trying to remove: " + quantity);
        }
        
        BigDecimal newQuantity = balance.getQuantity().subtract(quantity);
        BigDecimal newTotalCost = balance.getTotalCostEur().subtract(totalPriceEur);
        
        balance.setQuantity(newQuantity);
        balance.setTotalCostEur(newTotalCost);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            balance.setAveragePriceEur(newTotalCost.divide(newQuantity, 6, RoundingMode.CEILING));
        } else {
            balance.setAveragePriceEur(BigDecimal.ZERO);
            balance.setTotalCostEur(BigDecimal.ZERO);
        }
        
        DriverProductBalance saved = driverProductBalanceRepository.save(balance);
        
        if (saved.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            driverProductBalanceRepository.delete(saved);
            log.info("Driver balance deleted (quantity=0): id={}", saved.getId());
            return null;
        }
        
        log.info("Driver balance updated: id={}, newQuantity={}, newAveragePrice={}, totalCost={}", 
                saved.getId(), saved.getQuantity(), saved.getAveragePriceEur(), saved.getTotalCostEur());
        
        return saved;
    }
    
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
        
        if (oldQuantity != null && oldQuantity.compareTo(BigDecimal.ZERO) > 0 && oldTotalPrice != null) {
            balance.setTotalCostEur(balance.getTotalCostEur().subtract(oldTotalPrice));
            balance.setQuantity(balance.getQuantity().subtract(oldQuantity));
        }
        
        if (newQuantity != null && newQuantity.compareTo(BigDecimal.ZERO) > 0 && newTotalPrice != null) {
            BigDecimal newTotalCost = balance.getTotalCostEur().add(newTotalPrice);
            BigDecimal newQty = balance.getQuantity().add(newQuantity);
            
            balance.setQuantity(newQty);
            balance.setTotalCostEur(newTotalCost);
            
            if (newQty.compareTo(BigDecimal.ZERO) > 0) {
                balance.setAveragePriceEur(newTotalCost.divide(newQty, 6, RoundingMode.CEILING));
            }
        } else if (balance.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            balance.setAveragePriceEur(balance.getTotalCostEur().divide(balance.getQuantity(), 6, RoundingMode.CEILING));
        } else {
            balance.setQuantity(BigDecimal.ZERO);
            balance.setAveragePriceEur(BigDecimal.ZERO);
            balance.setTotalCostEur(BigDecimal.ZERO);
        }
        
        DriverProductBalance saved = driverProductBalanceRepository.save(balance);
        
        if (saved.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            driverProductBalanceRepository.delete(saved);
            log.info("Driver balance deleted after update (quantity=0): id={}", saved.getId());
            return null;
        }
        
        log.info("Driver balance updated: id={}, newQuantity={}, newAveragePrice={}, totalCost={}", 
                saved.getId(), saved.getQuantity(), saved.getAveragePriceEur(), saved.getTotalCostEur());
        
        return saved;
    }
    
    @Transactional(readOnly = true)
    public DriverProductBalance getBalance(Long driverId, Long productId) {
        return driverProductBalanceRepository.findByDriverIdAndProductId(driverId, productId).orElse(null);
    }
    
    @Transactional(readOnly = true)
    public List<DriverProductBalance> getDriverBalances(Long driverId) {
        return driverProductBalanceRepository.findByDriverId(driverId);
    }
    
    @Transactional(readOnly = true)
    public List<DriverProductBalance> getProductBalances(Long productId) {
        return driverProductBalanceRepository.findByProductId(productId);
    }
    
    @Transactional(readOnly = true)
    public boolean hasEnoughProduct(Long driverId, Long productId, BigDecimal requiredQuantity) {
        DriverProductBalance balance = getBalance(driverId, productId);
        if (balance == null) {
            return false;
        }
        return balance.getQuantity().compareTo(requiredQuantity) >= 0;
    }
    
    @Transactional(readOnly = true)
    public List<DriverProductBalance> getAllActiveBalances() {
        return driverProductBalanceRepository.findAllWithPositiveQuantity();
    }
    
    @Transactional
    public DriverProductBalance updateTotalCostEur(Long driverId, Long productId, BigDecimal newTotalCostEur) {
        log.info("Updating total cost EUR: driverId={}, productId={}, newTotalCostEur={}", 
                driverId, productId, newTotalCostEur);
        
        if (newTotalCostEur == null || newTotalCostEur.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total cost must be non-negative");
        }
        
        DriverProductBalance balance = driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseThrow(() -> new PurchaseException("BALANCE_NOT_FOUND", 
                        String.format("Driver balance not found: driverId=%d, productId=%d", driverId, productId)));
        
        balance.setTotalCostEur(newTotalCostEur);
        
        if (balance.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            balance.setAveragePriceEur(newTotalCostEur.divide(balance.getQuantity(), 6, RoundingMode.CEILING));
        } else {
            balance.setAveragePriceEur(BigDecimal.ZERO);
        }
        
        DriverProductBalance saved = driverProductBalanceRepository.save(balance);
        
        log.info("Driver balance total cost updated: id={}, newTotalCost={}, newAveragePrice={}", 
                saved.getId(), saved.getTotalCostEur(), saved.getAveragePriceEur());
        
        return saved;
    }
}

