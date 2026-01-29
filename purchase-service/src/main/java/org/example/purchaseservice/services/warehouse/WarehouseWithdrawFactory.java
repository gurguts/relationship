package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseWithdrawFactory {
    
    private final WarehouseWithdrawCalculator calculator;
    
    public void setWithdrawalDateIfNeeded(@NonNull WarehouseWithdrawal warehouseWithdrawal) {
        if (warehouseWithdrawal.getWithdrawalDate() == null) {
            warehouseWithdrawal.setWithdrawalDate(LocalDate.now());
        }
    }
    
    public void updateWithdrawalFields(
            @NonNull WarehouseWithdrawal withdrawal,
            @NonNull WarehouseWithdrawal request,
            @NonNull BigDecimal newQuantity,
            @NonNull BigDecimal unitPrice) {
        
        if (request.getWithdrawalReason() != null) {
            if (request.getWithdrawalReason().getPurpose() != WithdrawalReason.Purpose.REMOVING) {
                throw new PurchaseException("INVALID_WITHDRAWAL_REASON", 
                        "Withdrawal reason must have REMOVING purpose");
            }
            withdrawal.setWithdrawalReason(request.getWithdrawalReason());
        }
        
        if (request.getDescription() != null) {
            withdrawal.setDescription(request.getDescription());
        }
        
        if (request.getWithdrawalDate() != null) {
            withdrawal.setWithdrawalDate(request.getWithdrawalDate());
        }
        
        withdrawal.setQuantity(newQuantity);
        withdrawal.setUnitPriceEur(unitPrice);
        withdrawal.setTotalCostEur(calculator.calculateTotalCost(unitPrice, newQuantity));
    }
}
