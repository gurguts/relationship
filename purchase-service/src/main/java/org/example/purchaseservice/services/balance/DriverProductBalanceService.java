package org.example.purchaseservice.services.balance;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.repositories.DriverProductBalanceRepository;
import org.example.purchaseservice.services.impl.IDriverProductBalanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverProductBalanceService implements IDriverProductBalanceService {
    
    private final DriverProductBalanceRepository driverProductBalanceRepository;
    private final DriverProductBalanceValidator validator;
    private final DriverProductBalanceCalculator calculator;
    private final DriverProductBalanceHelper helper;
    
    @Override
    @Transactional
    public void addProduct(@NonNull Long driverId, @NonNull Long productId,
                           @NonNull BigDecimal quantity, @NonNull BigDecimal totalPriceEur) {
        log.info("Adding product to driver balance: driverId={}, productId={}, quantity={}, totalPriceEur={}",
                driverId, productId, quantity, totalPriceEur);

        validator.validateQuantity(quantity, "Added");
        validator.validateTotalPrice(totalPriceEur, "Total price");

        DriverProductBalance balance = helper.getOrCreateBalance(driverId, productId);

        BigDecimal newTotalCost = balance.getTotalCostEur().add(totalPriceEur);
        BigDecimal newQuantity = balance.getQuantity().add(quantity);

        calculator.updateBalanceValues(balance, newQuantity, newTotalCost);

        DriverProductBalance saved = driverProductBalanceRepository.save(balance);
        logBalanceUpdated(saved);
    }
    
    @Override
    @Transactional
    public void removeProduct(@NonNull Long driverId, @NonNull Long productId,
                              @NonNull BigDecimal quantity, @NonNull BigDecimal totalPriceEur) {
        log.info("Removing product from driver balance: driverId={}, productId={}, quantity={}, totalPrice={}",
                driverId, productId, quantity, totalPriceEur);

        validator.validateQuantity(quantity, "Removed");
        validator.validateTotalPrice(totalPriceEur, "Total price of removed purchase");

        DriverProductBalance balance = helper.getBalanceOrThrow(driverId, productId);
        validator.validateSufficientQuantity(balance.getQuantity(), quantity);

        BigDecimal newQuantity = balance.getQuantity().subtract(quantity);
        BigDecimal newTotalCost = calculator.ensureNonNegative(balance.getTotalCostEur().subtract(totalPriceEur));

        calculator.updateBalanceValues(balance, newQuantity, newTotalCost);

        DriverProductBalance saved = driverProductBalanceRepository.save(balance);
        Optional<DriverProductBalance> result = helper.deleteIfEmpty(saved);

        if (result.isEmpty()) {
            return;
        }

        logBalanceUpdated(saved);
    }
    
    @Override
    @Transactional
    public void updateFromPurchaseChange(@NonNull Long driverId, @NonNull Long productId,
                                         BigDecimal oldQuantity, BigDecimal oldTotalPrice,
                                         BigDecimal newQuantity, BigDecimal newTotalPrice) {
        log.info("Updating driver balance from purchase change: driverId={}, productId={}", driverId, productId);

        validator.validateQuantityNonNegative(oldQuantity, "Old quantity");
        validator.validateTotalPriceNonNegative(oldTotalPrice, "Old total price");
        validator.validateQuantityNonNegative(newQuantity, "New quantity");
        validator.validateTotalPriceNonNegative(newTotalPrice, "New total price");

        DriverProductBalance balance = helper.getOrCreateBalance(driverId, productId);

        if (oldQuantity != null && oldQuantity.compareTo(BigDecimal.ZERO) > 0 && oldTotalPrice != null) {
            BigDecimal currentTotalCost = calculator.ensureNonNegative(balance.getTotalCostEur().subtract(oldTotalPrice));
            BigDecimal currentQuantity = calculator.ensureNonNegative(balance.getQuantity().subtract(oldQuantity));

            balance.setTotalCostEur(currentTotalCost);
            balance.setQuantity(currentQuantity);
        }

        if (newQuantity != null && newQuantity.compareTo(BigDecimal.ZERO) > 0 && newTotalPrice != null) {
            BigDecimal newTotalCost = balance.getTotalCostEur().add(newTotalPrice);
            BigDecimal newQty = balance.getQuantity().add(newQuantity);

            calculator.updateBalanceValues(balance, newQty, newTotalCost);
        } else if (balance.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            balance.setAveragePriceEur(calculator.calculateAveragePrice(balance.getTotalCostEur(), balance.getQuantity()));
        } else {
            balance.setQuantity(BigDecimal.ZERO);
            balance.setAveragePriceEur(BigDecimal.ZERO);
            balance.setTotalCostEur(BigDecimal.ZERO);
        }

        DriverProductBalance saved = driverProductBalanceRepository.save(balance);
        Optional<DriverProductBalance> result = helper.deleteIfEmpty(saved);

        if (result.isEmpty()) {
            return;
        }

        logBalanceUpdated(saved);
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

        validator.validateTotalPrice(newTotalCostEur, "Total cost");

        DriverProductBalance balance = helper.getBalanceOrThrow(driverId, productId);

        balance.setTotalCostEur(newTotalCostEur);
        balance.setAveragePriceEur(calculator.calculateAveragePrice(newTotalCostEur, balance.getQuantity()));

        DriverProductBalance saved = driverProductBalanceRepository.save(balance);

        log.info("Driver balance total cost updated: id={}, newTotalCost={}, newAveragePrice={}",
                saved.getId(), saved.getTotalCostEur(), saved.getAveragePriceEur());

        return saved;
    }

    private void logBalanceUpdated(@NonNull DriverProductBalance balance) {
        log.info("Driver balance updated: id={}, newQuantity={}, newAveragePrice={}, totalCost={}",
                balance.getId(), balance.getQuantity(), balance.getAveragePriceEur(), balance.getTotalCostEur());
    }
}

