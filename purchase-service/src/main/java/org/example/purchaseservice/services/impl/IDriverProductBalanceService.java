package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.DriverProductBalance;

import java.math.BigDecimal;
import java.util.List;

public interface IDriverProductBalanceService {
    void addProduct(@NonNull Long driverId, @NonNull Long productId,
                    @NonNull BigDecimal quantity, @NonNull BigDecimal totalPriceEur);
    
    void removeProduct(@NonNull Long driverId, @NonNull Long productId,
                       @NonNull BigDecimal quantity, @NonNull BigDecimal totalPriceEur);
    
    void updateFromPurchaseChange(@NonNull Long driverId, @NonNull Long productId,
                                  BigDecimal oldQuantity, BigDecimal oldTotalPrice,
                                  BigDecimal newQuantity, BigDecimal newTotalPrice);
    
    DriverProductBalance getBalance(@NonNull Long driverId, @NonNull Long productId);
    
    List<DriverProductBalance> getDriverBalances(@NonNull Long driverId);
    
    List<DriverProductBalance> getProductBalances(@NonNull Long productId);

    List<DriverProductBalance> getAllActiveBalances();
    
    DriverProductBalance updateTotalCostEur(@NonNull Long driverId, @NonNull Long productId,
                                             @NonNull BigDecimal newTotalCostEur);
}
