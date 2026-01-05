package org.example.purchaseservice.services.balance;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.WarehouseBalanceAdjustment;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;

import java.math.BigDecimal;
import java.util.List;

public interface IWarehouseProductBalanceService {
    
    WarehouseProductBalance addProduct(@NonNull Long warehouseId, @NonNull Long productId, 
                                      @NonNull BigDecimal quantity, @NonNull BigDecimal totalCost);
    
    BigDecimal removeProduct(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal quantity);
    
    void addProductQuantityOnly(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal quantity);
    
    void removeProductWithCost(@NonNull Long warehouseId, @NonNull Long productId, 
                               @NonNull BigDecimal quantity, @NonNull BigDecimal totalCost);
    
    WarehouseProductBalance getBalance(@NonNull Long warehouseId, @NonNull Long productId);
    
    List<WarehouseProductBalance> getWarehouseBalances(@NonNull Long warehouseId);
    
    List<WarehouseProductBalance> getProductBalances(@NonNull Long productId);
    
    void adjustProductCost(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal costDelta);
    
    boolean hasEnoughProduct(@NonNull Long warehouseId, @NonNull Long productId, @NonNull BigDecimal requiredQuantity);
    
    BigDecimal getAveragePrice(@NonNull Long warehouseId, @NonNull Long productId);
    
    WarehouseProductBalance setInitialBalance(@NonNull Long warehouseId, @NonNull Long productId,
                                             @NonNull BigDecimal initialQuantity, @NonNull BigDecimal averagePriceEur);
    
    List<WarehouseProductBalance> getAllActiveBalances();
    
    WarehouseProductBalance updateBalance(@NonNull Long warehouseId, @NonNull Long productId,
                                         BigDecimal newQuantity, BigDecimal newTotalCost,
                                         Long userId, String description);
    
    List<WarehouseBalanceAdjustment> getBalanceAdjustments(@NonNull Long warehouseId, @NonNull Long productId);
}

