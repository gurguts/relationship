package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.services.impl.IDriverProductBalanceService;
import org.example.purchaseservice.services.impl.IWarehouseProductBalanceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseReceiptBalanceHandler {
    
    private final IDriverProductBalanceService driverProductBalanceService;
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    
    public void updateBalances(
            @NonNull WarehouseReceipt warehouseReceipt,
            @NonNull BigDecimal purchasedQuantity,
            @NonNull BigDecimal receivedQuantity,
            @NonNull BigDecimal totalDriverCost) {
        
        driverProductBalanceService.removeProduct(
                warehouseReceipt.getUserId(),
                warehouseReceipt.getProductId(),
                purchasedQuantity,
                totalDriverCost
        );

        warehouseProductBalanceService.addProduct(
                warehouseReceipt.getWarehouseId(),
                warehouseReceipt.getProductId(),
                receivedQuantity,
                totalDriverCost
        );
    }
}
