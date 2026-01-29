package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.repositories.WarehouseProductBalanceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseProductBalanceHelper {
    
    private final WarehouseProductBalanceRepository warehouseProductBalanceRepository;
    
    public WarehouseProductBalance getOrCreateBalance(Long warehouseId, Long productId) {
        return warehouseProductBalanceRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseGet(() -> createNewBalance(warehouseId, productId));
    }
    
    public WarehouseProductBalance createNewBalance(Long warehouseId, Long productId) {
        WarehouseProductBalance newBalance = new WarehouseProductBalance();
        newBalance.setWarehouseId(warehouseId);
        newBalance.setProductId(productId);
        newBalance.setQuantity(BigDecimal.ZERO);
        newBalance.setTotalCostEur(BigDecimal.ZERO);
        newBalance.setAveragePriceEur(BigDecimal.ZERO);
        return newBalance;
    }
    
    public BigDecimal getSafeTotalCost(WarehouseProductBalance balance) {
        return balance.getTotalCostEur() != null ? balance.getTotalCostEur() : BigDecimal.ZERO;
    }
    
    public BigDecimal getSafeQuantity(WarehouseProductBalance balance) {
        return balance.getQuantity() != null ? balance.getQuantity() : BigDecimal.ZERO;
    }
    
    public BigDecimal getSafeAveragePrice(WarehouseProductBalance balance) {
        return balance.getAveragePriceEur() != null ? balance.getAveragePriceEur() : BigDecimal.ZERO;
    }
    
    public void resetBalanceToZero(WarehouseProductBalance balance) {
        balance.setAveragePriceEur(BigDecimal.ZERO);
        balance.setTotalCostEur(BigDecimal.ZERO);
    }
    
    public void deleteBalanceIfEmpty(WarehouseProductBalance balance) {
        BigDecimal quantity = getSafeQuantity(balance);
        BigDecimal totalCost = getSafeTotalCost(balance);
        if (quantity.compareTo(BigDecimal.ZERO) == 0 && totalCost.compareTo(BigDecimal.ZERO) == 0) {
            warehouseProductBalanceRepository.delete(balance);
            log.info("Warehouse balance deleted (quantity=0 and totalCost=0): id={}", balance.getId());
        }
    }
    
    public boolean isEmpty(WarehouseProductBalance balance) {
        BigDecimal quantity = getSafeQuantity(balance);
        BigDecimal totalCost = getSafeTotalCost(balance);
        return quantity.compareTo(BigDecimal.ZERO) == 0 && totalCost.compareTo(BigDecimal.ZERO) == 0;
    }
}
