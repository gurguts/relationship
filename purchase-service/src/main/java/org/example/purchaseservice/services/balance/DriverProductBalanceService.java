package org.example.purchaseservice.services.balance;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.repositories.DriverProductBalanceRepository;
import org.example.purchaseservice.services.impl.IDriverProductBalanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverProductBalanceService implements IDriverProductBalanceService {
    
    private final DriverProductBalanceRepository driverProductBalanceRepository;
    
    @Override
    @Transactional
    public void addProduct(@NonNull Long driverId, @NonNull Long productId,
                           @NonNull BigDecimal quantity, @NonNull BigDecimal totalPriceEur) {
        log.info("Adding product to driver balance: driverId={}, productId={}, quantity={}, totalPriceEur={}",
                driverId, productId, quantity, totalPriceEur);

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Added quantity must be positive");
        }
        if (totalPriceEur.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TOTAL_PRICE", "Total price must be non-negative");
        }

        DriverProductBalance balance = driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseGet(() -> {
                    DriverProductBalance newBalance = new DriverProductBalance();
                    newBalance.setDriverId(driverId);
                    newBalance.setProductId(productId);
                    newBalance.setQuantity(BigDecimal.ZERO);
                    newBalance.setTotalCostEur(BigDecimal.ZERO);
                    newBalance.setAveragePriceEur(BigDecimal.ZERO);
                    return newBalance;
                });

        BigDecimal newTotalCost = balance.getTotalCostEur().add(totalPriceEur);
        BigDecimal newQuantity = balance.getQuantity().add(quantity);

        balance.setQuantity(newQuantity);
        balance.setTotalCostEur(newTotalCost);
        balance.setAveragePriceEur(calculateAveragePrice(newTotalCost, newQuantity));

        DriverProductBalance saved = driverProductBalanceRepository.save(balance);

        log.info("Driver balance updated: id={}, newQuantity={}, newAveragePrice={}, totalCost={}",
                saved.getId(), saved.getQuantity(), saved.getAveragePriceEur(), saved.getTotalCostEur());

    }
    
    @Override
    @Transactional
    public void removeProduct(@NonNull Long driverId, @NonNull Long productId,
                              @NonNull BigDecimal quantity, @NonNull BigDecimal totalPriceEur) {
        log.info("Removing product from driver balance: driverId={}, productId={}, quantity={}, totalPrice={}",
                driverId, productId, quantity, totalPriceEur);

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Removed quantity must be positive");
        }
        if (totalPriceEur.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TOTAL_PRICE", "Total price of removed purchase must be non-negative");
        }

        DriverProductBalance balance = driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseThrow(() -> new PurchaseException("BALANCE_NOT_FOUND",
                        String.format("Driver balance not found: driverId=%d, productId=%d", driverId, productId)));

        if (quantity.compareTo(balance.getQuantity()) > 0) {
            throw new PurchaseException("INSUFFICIENT_QUANTITY",
                    String.format("Cannot remove more than available quantity. Available: %s, trying to remove: %s",
                            balance.getQuantity(), quantity));
        }

        BigDecimal newQuantity = balance.getQuantity().subtract(quantity);
        BigDecimal newTotalCost = balance.getTotalCostEur().subtract(totalPriceEur);

        if (newTotalCost.compareTo(BigDecimal.ZERO) < 0) {
            newTotalCost = BigDecimal.ZERO;
        }

        balance.setQuantity(newQuantity);
        balance.setTotalCostEur(newTotalCost);
        balance.setAveragePriceEur(calculateAveragePrice(newTotalCost, newQuantity));

        DriverProductBalance saved = driverProductBalanceRepository.save(balance);

        if (saved.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            driverProductBalanceRepository.delete(saved);
            log.info("Driver balance deleted (quantity=0): id={}", saved.getId());
            return;
        }

        log.info("Driver balance updated: id={}, newQuantity={}, newAveragePrice={}, totalCost={}",
                saved.getId(), saved.getQuantity(), saved.getAveragePriceEur(), saved.getTotalCostEur());

    }
    
    @Override
    @Transactional
    public void updateFromPurchaseChange(@NonNull Long driverId, @NonNull Long productId,
                                         BigDecimal oldQuantity, BigDecimal oldTotalPrice,
                                         BigDecimal newQuantity, BigDecimal newTotalPrice) {
        log.info("Updating driver balance from purchase change: driverId={}, productId={}", driverId, productId);

        if (oldQuantity != null && oldQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Old quantity must be non-negative");
        }
        if (oldTotalPrice != null && oldTotalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TOTAL_PRICE", "Old total price must be non-negative");
        }
        if (newQuantity != null && newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_QUANTITY", "New quantity must be non-negative");
        }
        if (newTotalPrice != null && newTotalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TOTAL_PRICE", "New total price must be non-negative");
        }

        DriverProductBalance balance = driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseGet(() -> {
                    DriverProductBalance newBalance = new DriverProductBalance();
                    newBalance.setDriverId(driverId);
                    newBalance.setProductId(productId);
                    newBalance.setQuantity(BigDecimal.ZERO);
                    newBalance.setTotalCostEur(BigDecimal.ZERO);
                    newBalance.setAveragePriceEur(BigDecimal.ZERO);
                    return newBalance;
                });

        if (oldQuantity != null && oldQuantity.compareTo(BigDecimal.ZERO) > 0 && oldTotalPrice != null) {
            BigDecimal currentTotalCost = balance.getTotalCostEur().subtract(oldTotalPrice);
            BigDecimal currentQuantity = balance.getQuantity().subtract(oldQuantity);

            if (currentTotalCost.compareTo(BigDecimal.ZERO) < 0) {
                currentTotalCost = BigDecimal.ZERO;
            }
            if (currentQuantity.compareTo(BigDecimal.ZERO) < 0) {
                currentQuantity = BigDecimal.ZERO;
            }

            balance.setTotalCostEur(currentTotalCost);
            balance.setQuantity(currentQuantity);
        }

        if (newQuantity != null && newQuantity.compareTo(BigDecimal.ZERO) > 0 && newTotalPrice != null) {
            BigDecimal newTotalCost = balance.getTotalCostEur().add(newTotalPrice);
            BigDecimal newQty = balance.getQuantity().add(newQuantity);

            balance.setQuantity(newQty);
            balance.setTotalCostEur(newTotalCost);
            balance.setAveragePriceEur(calculateAveragePrice(newTotalCost, newQty));
        } else if (balance.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            balance.setAveragePriceEur(calculateAveragePrice(balance.getTotalCostEur(), balance.getQuantity()));
        } else {
            balance.setQuantity(BigDecimal.ZERO);
            balance.setAveragePriceEur(BigDecimal.ZERO);
            balance.setTotalCostEur(BigDecimal.ZERO);
        }

        DriverProductBalance saved = driverProductBalanceRepository.save(balance);

        if (saved.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            driverProductBalanceRepository.delete(saved);
            log.info("Driver balance deleted after update (quantity=0): id={}", saved.getId());
            return;
        }

        log.info("Driver balance updated: id={}, newQuantity={}, newAveragePrice={}, totalCost={}",
                saved.getId(), saved.getQuantity(), saved.getAveragePriceEur(), saved.getTotalCostEur());

    }
    
    @Override
    @Transactional(readOnly = true)
    public DriverProductBalance getBalance(@NonNull Long driverId, @NonNull Long productId) {
        return driverProductBalanceRepository.findByDriverIdAndProductId(driverId, productId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverProductBalance> getDriverBalances(@NonNull Long driverId) {
        return driverProductBalanceRepository.findByDriverId(driverId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverProductBalance> getProductBalances(@NonNull Long productId) {
        return driverProductBalanceRepository.findByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverProductBalance> getAllActiveBalances() {
        return driverProductBalanceRepository.findAllWithPositiveQuantity();
    }
    
    @Override
    @Transactional
    public DriverProductBalance updateTotalCostEur(@NonNull Long driverId, @NonNull Long productId,
                                                    @NonNull BigDecimal newTotalCostEur) {
        log.info("Updating total cost EUR: driverId={}, productId={}, newTotalCostEur={}",
                driverId, productId, newTotalCostEur);

        if (newTotalCostEur.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TOTAL_COST", "Total cost must be non-negative");
        }

        DriverProductBalance balance = driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseThrow(() -> new PurchaseException("BALANCE_NOT_FOUND",
                        String.format("Driver balance not found: driverId=%d, productId=%d", driverId, productId)));

        balance.setTotalCostEur(newTotalCostEur);
        balance.setAveragePriceEur(calculateAveragePrice(newTotalCostEur, balance.getQuantity()));

        DriverProductBalance saved = driverProductBalanceRepository.save(balance);

        log.info("Driver balance total cost updated: id={}, newTotalCost={}, newAveragePrice={}",
                saved.getId(), saved.getTotalCostEur(), saved.getAveragePriceEur());

        return saved;
    }

    private BigDecimal calculateAveragePrice(@NonNull BigDecimal totalCost, @NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            return totalCost.divide(quantity, 6, RoundingMode.CEILING);
        }
        return BigDecimal.ZERO;
    }
}

