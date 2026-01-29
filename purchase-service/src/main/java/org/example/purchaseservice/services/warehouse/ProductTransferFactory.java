package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferUpdateDTO;
import org.example.purchaseservice.models.warehouse.ProductTransfer;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.example.purchaseservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTransferFactory {

    private final WithdrawalReasonRepository withdrawalReasonRepository;
    private final ProductTransferCalculator calculator;
    
    public ProductTransfer createTransfer(
            @NonNull ProductTransferDTO transferDTO,
            @NonNull BigDecimal unitPrice,
            @NonNull BigDecimal totalCost) {
        
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new PurchaseException("USER_NOT_FOUND", "Current user ID is null");
        }
        
        WithdrawalReason reason = withdrawalReasonRepository.findById(transferDTO.getWithdrawalReasonId())
                .orElseThrow(() -> new PurchaseException("REASON_NOT_FOUND",
                        "Reason not found: " + transferDTO.getWithdrawalReasonId()));
        
        ProductTransfer transfer = new ProductTransfer();
        transfer.setWarehouseId(transferDTO.getWarehouseId());
        transfer.setFromProductId(transferDTO.getFromProductId());
        transfer.setToProductId(transferDTO.getToProductId());
        transfer.setQuantity(transferDTO.getQuantity());
        transfer.setUnitPriceEur(unitPrice);
        transfer.setTotalCostEur(totalCost);
        transfer.setTransferDate(LocalDate.now());
        transfer.setUserId(userId);
        transfer.setReason(reason);
        transfer.setDescription(transferDTO.getDescription());
        
        return transfer;
    }
    
    public void updateTransferFields(
            @NonNull ProductTransfer transfer,
            @NonNull ProductTransferUpdateDTO updateDTO,
            @NonNull BigDecimal newQuantity,
            @NonNull BigDecimal unitPrice) {
        
        if (updateDTO.getReasonId() != null &&
                (transfer.getReason() == null || !updateDTO.getReasonId().equals(transfer.getReason().getId()))) {
            WithdrawalReason reason = withdrawalReasonRepository.findById(updateDTO.getReasonId())
                    .orElseThrow(() -> new PurchaseException("REASON_NOT_FOUND",
                            "Reason not found: " + updateDTO.getReasonId()));

            if (reason.getPurpose() != WithdrawalReason.Purpose.BOTH) {
                throw new PurchaseException("INVALID_REASON_PURPOSE",
                        "For transfers, only reasons with BOTH purpose type can be selected");
            }

            transfer.setReason(reason);
        }

        if (updateDTO.getDescription() != null) {
            transfer.setDescription(updateDTO.getDescription());
        }

        transfer.setQuantity(newQuantity);
        transfer.setUnitPriceEur(unitPrice);
        transfer.setTotalCostEur(calculator.calculateTotalCost(newQuantity, unitPrice));
    }
    
    public BigDecimal getSafeQuantity(@NonNull ProductTransfer transfer) {
        BigDecimal quantity = transfer.getQuantity();
        if (quantity == null) {
            throw new PurchaseException("INVALID_TRANSFER", "Transfer quantity cannot be null");
        }
        return quantity;
    }
}
