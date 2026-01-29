package org.example.purchaseservice.services.warehouse;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.warehouse.ProductTransfer;
import org.example.purchaseservice.services.impl.IWarehouseProductBalanceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTransferBalanceHandler {
    
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    @Getter
    private final ProductTransferValidator validator;
    
    public void validateAndCheckBalance(Long warehouseId, Long productId, BigDecimal quantity) {
        if (warehouseId == null) {
            throw new PurchaseException("INVALID_WAREHOUSE_ID", "Warehouse ID cannot be null");
        }
        if (productId == null) {
            throw new PurchaseException("INVALID_PRODUCT_ID", "Product ID cannot be null");
        }
        if (quantity == null) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be null");
        }
        
        if (!warehouseProductBalanceService.hasEnoughProduct(warehouseId, productId, quantity)) {
            var balance = warehouseProductBalanceService.getBalance(warehouseId, productId);
            throw new PurchaseException("INSUFFICIENT_PRODUCT",
                    String.format("Insufficient product on warehouse. Available: %s, requested: %s",
                            balance != null ? balance.getQuantity() : BigDecimal.ZERO, quantity));
        }
    }
    
    public void transferProductBetweenBalances(
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            BigDecimal quantity,
            BigDecimal totalCost) {
        
        warehouseProductBalanceService.removeProductWithCost(warehouseId, fromProductId, quantity, totalCost);
        warehouseProductBalanceService.addProduct(warehouseId, toProductId, quantity, totalCost);
    }
    
    public void handleQuantityIncrease(
            @NonNull ProductTransfer transfer,
            @NonNull BigDecimal additionalQuantity,
            @NonNull BigDecimal unitPrice,
            @NonNull ProductTransferCalculator calculator) {
        
        BigDecimal additionalCost = calculator.calculateTotalCost(additionalQuantity, unitPrice);
        validateAndCheckBalance(transfer.getWarehouseId(), transfer.getFromProductId(), additionalQuantity);
        transferProductBetweenBalances(
                transfer.getWarehouseId(),
                transfer.getFromProductId(),
                transfer.getToProductId(),
                additionalQuantity,
                additionalCost
        );
    }
    
    public void handleQuantityDecrease(
            @NonNull ProductTransfer transfer,
            @NonNull BigDecimal quantityToReturn,
            @NonNull BigDecimal unitPrice,
            @NonNull ProductTransferCalculator calculator) {
        
        BigDecimal costToReturn = calculator.calculateTotalCost(quantityToReturn, unitPrice);
        validateAndCheckBalance(transfer.getWarehouseId(), transfer.getToProductId(), quantityToReturn);
        transferProductBetweenBalances(
                transfer.getWarehouseId(),
                transfer.getToProductId(),
                transfer.getFromProductId(),
                quantityToReturn,
                costToReturn
        );
    }

}
