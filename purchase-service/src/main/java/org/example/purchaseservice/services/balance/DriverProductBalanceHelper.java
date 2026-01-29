package org.example.purchaseservice.services.balance;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.repositories.DriverProductBalanceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverProductBalanceHelper {

    private final DriverProductBalanceRepository driverProductBalanceRepository;

    public DriverProductBalance getOrCreateBalance(@NonNull Long driverId, @NonNull Long productId) {
        return driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseGet(() -> createNewBalance(driverId, productId));
    }

    public DriverProductBalance getBalanceOrThrow(@NonNull Long driverId, @NonNull Long productId) {
        return driverProductBalanceRepository
                .findByDriverIdAndProductId(driverId, productId)
                .orElseThrow(() -> new org.example.purchaseservice.exceptions.PurchaseException("BALANCE_NOT_FOUND",
                        String.format("Driver balance not found: driverId=%d, productId=%d", driverId, productId)));
    }

    public Optional<DriverProductBalance> deleteIfEmpty(@NonNull DriverProductBalance balance) {
        if (balance.getQuantity() != null && balance.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            driverProductBalanceRepository.delete(balance);
            log.info("Driver balance deleted (quantity=0): id={}", balance.getId());
            return Optional.empty();
        }
        return Optional.of(balance);
    }

    private DriverProductBalance createNewBalance(@NonNull Long driverId, @NonNull Long productId) {
        DriverProductBalance newBalance = new DriverProductBalance();
        newBalance.setDriverId(driverId);
        newBalance.setProductId(productId);
        newBalance.setQuantity(BigDecimal.ZERO);
        newBalance.setTotalCostEur(BigDecimal.ZERO);
        newBalance.setAveragePriceEur(BigDecimal.ZERO);
        return newBalance;
    }
}
